mod commands;
mod config;

use clap::{Parser, Subcommand};
use config::{Config, get_config_path};
use std::fs;

#[derive(Parser)]
#[command(name = "anisub-cli")]
#[command(about = "Anisub CLI Uygulaması")]
#[command(arg_required_else_help = true)]
#[command(disable_help_subcommand = true)]
#[command(help_template = "\
{about}

Kullanım: {usage}

Komutlar:
{subcommands}

Seçenekler:
{options}
")]
struct Cli {
    #[command(subcommand)]
    command: Option<Commands>,
}

#[derive(Subcommand)]
enum Commands {
    #[command(about = "API Token'ınızı kullanarak hesabınıza giriş yapın")]
    Login,
    #[command(about = "Altyazılarda arama yapın")]
    Search(commands::search::SearchArgs),
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let cli = Cli::parse();
    let config_path = get_config_path();
    let mut config = Config::default();

    if config_path.exists() {
        let config_content = fs::read_to_string(&config_path)?;
        if let Ok(parsed_config) = json5::from_str::<Config>(&config_content) {
            config = parsed_config;
        }
    }

    match &cli.command {
        Some(Commands::Login) => {
            commands::login::execute(&config_path, config)?;
        }
        Some(Commands::Search(args)) => {
            commands::search::execute(&config, args)?;
        }
        None => {}
    }

    Ok(())
}
