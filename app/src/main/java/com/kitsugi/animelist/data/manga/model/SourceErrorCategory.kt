package com.kitsugi.animelist.data.manga.model

enum class SourceErrorCategory {
    None,
    Timeout,
    RateLimited,
    Captcha,
    NotFound,
    Network,
    Auth,
    Unknown,
}
