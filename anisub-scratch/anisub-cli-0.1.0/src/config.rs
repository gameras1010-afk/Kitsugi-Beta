use serde::{Deserialize, Serialize};
use std::path::PathBuf;

#[derive(Serialize, Deserialize, Default, Debug, Clone)]
pub struct Config {
    pub token: Option<String>,
}

#[derive(Deserialize, Debug)]
#[allow(dead_code)]
pub struct ApiResponse<T> {
    pub success: bool,
    pub data: Option<T>,
    pub message: Option<String>,
}

pub fn get_config_path() -> PathBuf {
    dirs::home_dir()
        .expect("Home dizini bulunamadı.")
        .join(".anisub-cli")
}
