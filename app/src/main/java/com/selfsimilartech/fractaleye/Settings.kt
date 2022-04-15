package com.selfsimilartech.fractaleye

object Settings {

        var editMode                    = EditMode.POSITION
        var goldEnabled                 = false

        var resolution                  = Resolution.R1440
        var aspectRatio                 = AspectRatio.RATIO_SCREEN

        var targetFramerate             = 60
        var perturbPrecision            = 64L
        var gpuPrecision                = GpuPrecision.SINGLE
        var cpuPrecision                = CpuPrecision.DOUBLE
        var hardwareProfile             = HardwareProfile.GPU

        var chunkProfile                = ChunkProfile.MED
        var useAlternateSplit           = false
        var autoPrecision               = true
        var continuousPosRender         = true
        var renderBackground            = true
        var continuousParamRender       = true
        var showProgress                = true
        var hideSystemBars              = true
        var sampleOnStrictTranslate     = true

        var advancedSettingsEnabled     = false
        var allowSlowDualfloat          = false
        var ultraHighResolutions        = false
        var restrictParams              = true

        var fitToViewport               = false
        var buttonAlignment             = ButtonAlignment.RIGHT
        var colorListViewType           = ListLayoutType.GRID
        var shapeListViewType           = ListLayoutType.GRID
        var textureListViewType         = ListLayoutType.GRID
        var bookmarkListViewType        = ListLayoutType.GRID
        var autofitColorRange           = false

}