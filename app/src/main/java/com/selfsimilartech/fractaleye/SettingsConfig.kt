package com.selfsimilartech.fractaleye

class SettingsConfig (
        var goldEnabled              : Boolean          = false,
        var resolution               : Resolution       = Resolution.R1440,
        var aspectRatio              : AspectRatio      = AspectRatio.RATIO_SCREEN,
        var gpuPrecision             : GpuPrecision     = GpuPrecision.SINGLE,
        var cpuPrecision             : CpuPrecision     = CpuPrecision.DOUBLE,
        var perturbPrecision         : Long             = 64L,
        var hardwareProfile          : HardwareProfile  = HardwareProfile.GPU,
        var useAlternateSplit        : Boolean          = false,
        var chunkProfile             : ChunkProfile     = ChunkProfile.MED,
        var autoPrecision            : Boolean          = true,
        var continuousPosRender      : Boolean          = false,
        var continuousParamRender    : Boolean          = true,
        var displayParams            : Boolean          = false,
        var showProgress             : Boolean          = true,
        var sampleOnStrictTranslate  : Boolean          = true,
        var renderBackground         : Boolean          = true,
        var fitToViewport            : Boolean          = false,
        var hideNavBar               : Boolean          = true,
        var showHints                : Boolean          = true,
        var colorListViewType        : ListLayoutType   = ListLayoutType.GRID,
        var shapeListViewType        : ListLayoutType   = ListLayoutType.GRID,
        var textureListViewType      : ListLayoutType   = ListLayoutType.GRID,
        var autofitColorRange        : Boolean          = false
)