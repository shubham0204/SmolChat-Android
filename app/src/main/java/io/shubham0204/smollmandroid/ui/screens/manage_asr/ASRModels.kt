package io.shubham0204.smollmandroid.ui.screens.manage_asr

val availableASRModels = listOf(
    ASRModel(
        name = "tiny-en",
        description = "Tiny Model - English",
        bundleDownloadUrl = "https://huggingface.co/shubhxm0204/moonshine-asr-models/resolve/main/tiny-en.zip",
        bundleFileName = "tiny-en.zip",
        arch = ASRModelArch.TINY
    ),
    ASRModel(
        name = "base-zh",
        description = "Base Model - Chinese",
        bundleDownloadUrl = "https://huggingface.co/shubhxm0204/moonshine-asr-models/resolve/main/base-zh.zip",
        bundleFileName = "base-zh.zip",
        arch = ASRModelArch.BASE
    ),
    ASRModel(
        name = "base-en",
        description = "Base Model - English",
        bundleDownloadUrl = "https://huggingface.co/shubhxm0204/moonshine-asr-models/resolve/main/base-en.zip",
        bundleFileName = "base-en.zip",
        arch = ASRModelArch.BASE
    ),
)

data class ASRModel(
    val name: String,
    val description: String,
    val bundleDownloadUrl: String,
    val bundleFileName: String,
    val arch: ASRModelArch
)

enum class ASRModelArch {
    TINY,
    BASE
}