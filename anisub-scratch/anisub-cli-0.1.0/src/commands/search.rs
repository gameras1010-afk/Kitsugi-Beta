use crate::config::Config;
use clap::Args;
use crossterm::{
    cursor,
    event::{self, Event, KeyCode, KeyModifiers},
    execute, queue,
    style::{Color, Print, ResetColor, SetBackgroundColor, SetForegroundColor},
    terminal::{self, disable_raw_mode, enable_raw_mode, Clear, ClearType},
};
use reqwest::blocking::Client;
use scraper::{Html, Selector};
use serde::Deserialize;
use std::error::Error;
use std::io::{stdout, Read, Write};
use std::path::PathBuf;

#[derive(Args)]
pub struct SearchArgs {
    #[arg(help = "Arama terimi (altyazı başlığı veya anime adı)")]
    pub query: String,
    
    #[arg(short = 'o', long = "out", help = "İndirme dizini (Varsayılan: mevcut dizin)")]
    pub output_dir: Option<PathBuf>,
}

#[derive(Deserialize, Debug, Clone)]
struct InertiaProps {
    subtitles: SubtitlesData,
}

#[derive(Deserialize, Debug, Clone)]
struct SubtitlesData {
    data: Vec<SubtitleData>,
    #[allow(dead_code)]
    current_page: u32,
    last_page: u32,
}

#[derive(Deserialize, Debug, Clone)]
struct SubtitleData {
    subtitle_id: u64,
    subtitle_release_name: String,
    subtitle_language: String,
    download_count: u64,
    #[serde(rename = "mediaInfo")]
    media_info: MediaInfo,
}

#[derive(Deserialize, Debug, Clone)]
struct MediaInfo {
    title_english: Option<String>,
    title_romaji: Option<String>,
}

#[derive(Deserialize, Debug)]
struct InertiaResponse {
    props: InertiaProps,
}

#[derive(Deserialize, Debug)]
struct DownloadResponse {
    success: bool,
    data: Option<DownloadData>,
    message: Option<String>,
}

#[derive(Deserialize, Debug)]
struct DownloadData {
    download_url: String,
    filename: String,
    file_size: u64,
}

fn fetch_page(
    client: &Client,
    config: &Config,
    query: &str,
    version: &str,
    page: u32,
) -> Result<(Vec<SubtitleData>, u32), Box<dyn Error>> {
    let url = format!("https://anisub.co/tum-altyazilar?q={}&page={}", query, page);
    
    let mut request = client
        .get(&url)
        .header("x-inertia", "true")
        .header("x-inertia-version", version)
        .header("x-requested-with", "XMLHttpRequest");

    if let Some(token) = &config.token {
        request = request.header("Authorization", format!("Bearer {}", token));
    }

    let response = request.send()?;
    
    if !response.status().is_success() {
        return Err(format!("HTTP Hatası: {}", response.status()).into());
    }

    let result: InertiaResponse = response.json()?;
    Ok((result.props.subtitles.data, result.props.subtitles.last_page))
}

pub fn execute(config: &Config, args: &SearchArgs) -> Result<(), Box<dyn Error>> {
    println!("'{}' için arama yapılıyor...\n", args.query);
    
    let client = Client::builder()
        .user_agent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36")
        .cookie_store(true)
        .build()?;

    let init_resp = client.get("https://anisub.co").send()?.text()?;
    
    let document = Html::parse_document(&init_resp);
    let selector = Selector::parse(r#"script[data-page="app"]"#).unwrap();
    let mut version = String::new();

    if let Some(script_tag) = document.select(&selector).next() {
        let json_text = script_tag.inner_html();
        if let Ok(parsed) = serde_json::from_str::<serde_json::Value>(&json_text) {
            if let Some(v) = parsed.get("version").and_then(|v| v.as_str()) {
                version = v.to_string();
            }
        }
    }

    let (mut items, mut total_pages) = fetch_page(&client, config, &args.query, &version, 1)?;
    let mut current_page = 1;

    if items.is_empty() {
        println!("Arama sonucunda hiçbir altyazı bulunamadı.");
        return Ok(());
    }

    let mut stdout = stdout();
    enable_raw_mode()?;
    let _ = execute!(stdout, cursor::Hide, terminal::EnterAlternateScreen);

    let mut selected = 0;
    let mut scroll = 0;
    let mut result_item: Option<SubtitleData> = None;

    loop {
        let (cols, rows) = terminal::size().unwrap_or((80, 24));
        let list_height = rows.saturating_sub(2) as usize;

        if selected < scroll {
            scroll = selected;
        } else if selected >= scroll + list_height {
            scroll = selected - list_height + 1;
        }

        if selected + 10 >= items.len() && current_page < total_pages {
            let title_loading = format!(
                "Arama terimi: '{}' | Sayfa: {}/{} | * Sonraki sayfa yükleniyor... *",
                args.query,
                current_page,
                total_pages
            );
            
            let _ = queue!(stdout, Clear(ClearType::All), cursor::MoveTo(0, 0), Print(title_loading), cursor::MoveToNextLine(1));

            for i in 0..list_height {
                let item_idx = scroll + i;
                if item_idx >= items.len() {
                    break;
                }
                let item = &items[item_idx];
                
                let romaji = item.media_info.title_romaji.clone().unwrap_or_default();
                let english = item.media_info.title_english.clone().unwrap_or_default();
                
                let anime_title = if !english.is_empty() {
                    english
                } else if !romaji.is_empty() {
                    romaji
                } else {
                    "Bilinmeyen Anime".to_string()
                };

                let mut line = format!(
                    "ID:{:<5} | {:<7} | {} - {} (↓{})",
                    item.subtitle_id,
                    item.subtitle_language,
                    anime_title,
                    item.subtitle_release_name,
                    item.download_count
                );
                
                if line.chars().count() > cols as usize {
                    line = line.chars().take(cols as usize - 3).collect::<String>() + "...";
                }
                
                if item_idx == selected {
                    let _ = queue!(
                        stdout,
                        SetBackgroundColor(Color::White),
                        SetForegroundColor(Color::Black),
                        Print(line),
                        ResetColor,
                        cursor::MoveToNextLine(1)
                    );
                } else {
                    let _ = queue!(stdout, Print(line), cursor::MoveToNextLine(1));
                }
            }
            let _ = stdout.flush();
            
            current_page += 1;
            if let Ok((mut new_items, new_total)) = fetch_page(&client, config, &args.query, &version, current_page) {
                items.append(&mut new_items);
                total_pages = new_total;
            } else {
                current_page -= 1;
            }
        }

        let _ = queue!(stdout, Clear(ClearType::All), cursor::MoveTo(0, 0));
        let title = format!(
            "Arama terimi: '{}' | Sayfa: {}/{} | Ok tuşları ile gezin, Enter: Seç, Q: Çık",
            args.query,
            current_page,
            total_pages
        );
        
        let _ = queue!(stdout, Print(title), cursor::MoveToNextLine(1));

        for i in 0..list_height {
            let item_idx = scroll + i;
            if item_idx >= items.len() {
                break;
            }
            let item = &items[item_idx];
            
            let romaji = item.media_info.title_romaji.clone().unwrap_or_default();
            let english = item.media_info.title_english.clone().unwrap_or_default();
            
            let anime_title = if !english.is_empty() {
                english
            } else if !romaji.is_empty() {
                romaji
            } else {
                "Bilinmeyen Anime".to_string()
            };

            let mut line = format!(
                "ID:{:<5} | {:<7} | {} - {} (↓{})",
                item.subtitle_id,
                item.subtitle_language,
                anime_title,
                item.subtitle_release_name,
                item.download_count
            );
            
            if line.chars().count() > cols as usize {
                line = line.chars().take(cols as usize - 3).collect::<String>() + "...";
            }
            
            if item_idx == selected {
                let _ = queue!(
                    stdout,
                    SetBackgroundColor(Color::White),
                    SetForegroundColor(Color::Black),
                    Print(line),
                    ResetColor,
                    cursor::MoveToNextLine(1)
                );
            } else {
                let _ = queue!(stdout, Print(line), cursor::MoveToNextLine(1));
            }
        }
        let _ = stdout.flush();

        if event::poll(std::time::Duration::from_millis(100)).unwrap_or(false) {
            if let Ok(Event::Key(key)) = event::read() {
                match key.code {
                    KeyCode::Char('q') | KeyCode::Esc => break,
                    KeyCode::Char('c') if key.modifiers.contains(KeyModifiers::CONTROL) => break,
                    KeyCode::Up => {
                        if selected > 0 {
                            selected -= 1;
                        }
                    }
                    KeyCode::Down => {
                        if selected < items.len() - 1 {
                            selected += 1;
                        }
                    }
                    KeyCode::Enter => {
                        result_item = Some(items[selected].clone());
                        break;
                    }
                    _ => {}
                }
            }
        }
    }

    let _ = execute!(stdout, cursor::Show, terminal::LeaveAlternateScreen);
    let _ = disable_raw_mode();

    if let Some(item) = result_item {
        let download_api_url = if config.token.is_some() {
            format!("https://anisub.co/api/integrations/subtitles/{}/download", item.subtitle_id)
        } else {
            format!("https://anisub.co/api/subtitles/{}/download", item.subtitle_id)
        };

        let mut req = client.get(&download_api_url);
        if let Some(token) = &config.token {
            req = req.header("Authorization", format!("Bearer {}", token));
        }

        let dl_resp = req.send()?;
        
        if !dl_resp.status().is_success() {
            println!("İndirme bilgisi alınamadı (HTTP {})", dl_resp.status());
            return Ok(());
        }

        let dl_info: DownloadResponse = dl_resp.json()?;
        if !dl_info.success {
            println!("Hata: {}", dl_info.message.unwrap_or_else(|| "Bilinmeyen bir hata oluştu.".to_string()));
            return Ok(());
        }

        if let Some(dl_data) = dl_info.data {
            let dest_dir = args.output_dir.clone().unwrap_or_else(|| std::env::current_dir().unwrap());
            if !dest_dir.exists() {
                std::fs::create_dir_all(&dest_dir)?;
            }
            
            let dest_path = dest_dir.join(&dl_data.filename);
            let mut file = std::fs::File::create(&dest_path)?;

            let mut res = client.get(&dl_data.download_url).send()?;
            if !res.status().is_success() {
                println!("Dosya indirilemedi (HTTP {})", res.status());
                return Ok(());
            }

            let total_size = res.content_length().unwrap_or(dl_data.file_size);
            let mut downloaded: u64 = 0;
            let mut buffer = [0; 8192];
            let mut stdout_handle = std::io::stdout();

            loop {
                let n = res.read(&mut buffer)?;
                if n == 0 { break; }
                file.write_all(&buffer[..n])?;
                downloaded += n as u64;

                let percent = if total_size > 0 {
                    (downloaded as f64 / total_size as f64) * 100.0
                } else {
                    0.0
                };

                print!("\r[{:>3.0}%] {} indiriliyor...", percent, dl_data.filename);
                stdout_handle.flush()?;
            }
            
            println!("\r[100%] {} başarıyla {} konumuna indirildi.          ", dl_data.filename, dest_dir.display());
        }
    }

    Ok(())
}

#[allow(dead_code)]
pub fn execute_api(config: &Config, args: &SearchArgs) -> Result<(), Box<dyn Error>> {
    let client = Client::new();
    let url = "https://anisub.co/api/subtitles";
    
    let mut request = client.get(url)
        .query(&[("q", &args.query)]);

    if let Some(token) = &config.token {
        request = request.header("Authorization", format!("Bearer {}", token));
    }

    let _response = request.send()?;
    
    Ok(())
}
