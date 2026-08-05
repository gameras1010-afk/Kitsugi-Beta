use crate::config::Config;
use crossterm::{
    event::{self, Event, KeyCode, KeyModifiers},
    terminal::{disable_raw_mode, enable_raw_mode},
};
use std::fs;
use std::io::{self, Write};
use std::path::PathBuf;

fn read_masked_token() -> io::Result<String> {
    let mut token = String::new();
    enable_raw_mode()?;
    
    loop {
        if event::poll(std::time::Duration::from_millis(500))? {
            if let Event::Key(key) = event::read()? {
                match key.code {
                    KeyCode::Char(c) => {
                        if key.modifiers.contains(KeyModifiers::CONTROL) && c == 'c' {
                            disable_raw_mode()?;
                            std::process::exit(0);
                        }
                        
                        token.push(c);
                        
                        if token.len() <= 5 {
                            print!("{}", c);
                        } else {
                            print!("*");
                        }
                        io::stdout().flush()?;
                    }
                    KeyCode::Backspace => {
                        if token.pop().is_some() {
                            print!("\x08 \x08"); 
                            io::stdout().flush()?;
                        }
                    }
                    KeyCode::Enter => {
                        println!("\r"); 
                        break;
                    }
                    _ => {}
                }
            }
        }
    }
    
    disable_raw_mode()?;
    Ok(token.trim().to_string())
}

fn ask_confirmation() -> io::Result<bool> {
    loop {
        print!("Token girmediniz. Bu önerilmez, emin misiniz? (e/h): ");
        io::stdout().flush()?;
        
        let mut input = String::new();
        io::stdin().read_line(&mut input)?;
        let input = input.trim().to_lowercase();
        
        match input.as_str() {
            "e" | "evet" | "y" | "yes" => return Ok(true),
            "h" | "hayir" | "hayır" | "n" | "no" => return Ok(false),
            _ => println!("Lütfen geçerli bir seçenek girin: 'e' / 'y' (evet) veya 'h' / 'n' (hayır)."),
        }
    }
}

pub fn execute(config_path: &PathBuf, mut config: Config) -> Result<(), Box<dyn std::error::Error>> {
    println!("Anisub CLI\n");

    let had_token = config.token.is_some();

    if had_token {
        println!("Daha önce giriş yaptınız. Yeni bir token girmek isterseniz devam edin.");
    }
    
    loop {
        print!("Lütfen API Token'ınızı girin (Boş bırakmak için Enter'a basın): ");
        io::stdout().flush()?;
        
        let token = read_masked_token()?;
        
        if token.is_empty() {
            if ask_confirmation()? {
                config.token = None;
                break; 
            }
        } else {
            config.token = Some(token);
            break;
        }
    }

    let config_json5 = json5::to_string(&config)?;
    fs::write(config_path, config_json5)?;

    if config.token.is_some() {
        println!("Başarıyla giriş yapıldı.");
    } else if had_token {
        println!("Mevcut token silindi. Token olmadan devam ediliyor.");
    } else {
        println!("Token olmadan devam ediliyor.");
    }

    Ok(())
}
