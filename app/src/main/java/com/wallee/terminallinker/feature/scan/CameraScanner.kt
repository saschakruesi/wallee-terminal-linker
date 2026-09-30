package com.wallee.terminallinker.feature.scan

import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max

/** Barcode formats for the two scanner modes (docs/03 §Scanner). */
object ScanFormats {
    val SERIAL: List<Int> = listOf(
        Barcode.FORMAT_CODE_128,
        Barcode.FORMAT_CODE_39,
        Barcode.FORMAT_CODE_93,
        Barcode.FORMAT_QR_CODE,
        Barcode.FORMAT_DATA_MATRIX,
        Barcode.FORMAT_EAN_13,
    )
    val QR_ONLY: List<Int> = listOf(Barcode.FORMAT_QR_CODE)
}

/**
 * Source of scanned values, so the scan screen can run with a fake in UI tests (docs/05 Phase 5.6).
 * The only production implementation is [CameraBarcodeScanner].
 */
fun interface BarcodeScanner {
    @Composable
    fun Content(
        formats: List<Int>,
        viewfinder: () -> Rect?,
        paused: Boolean,
        torch: Boolean,
        pick: (List<String>) -> String?,
        onDetected: (String) -> Unit,
        modifier: Modifier,
    )
}

/** The real camera: CameraX + ML Kit via [CameraScanner]. */
object CameraBarcodeScanner : BarcodeScanner {
    @Composable
    override fun Content(
        formats: List<Int>,
        viewfinder: () -> Rect?,
        paused: Boolean,
        torch: Boolean,
        pick: (List<String>) -> String?,
        onDetected: (String) -> Unit,
        modifier: Modifier,
    ) = CameraScanner(formats, viewfinder, paused, torch, onDetected, modifier, pick)
}

/**
 * CameraX preview + ML Kit analyzer, encapsulated without any API dependency (docs/05 Phase 4.1).
 * Only barcodes whose centre lies inside [viewfinder] (preview pixels) count; the first value seen twice in
 * a row is reported once via [onDetected]. While [paused] frames are dropped. [torch] toggles the flash.
 * When several codes lie inside the viewfinder, [pick] chooses among their raw values (default: the first).
 */
@Composable
fun CameraScanner(
    formats: List<Int>,
    viewfinder: () -> Rect?,
    paused: Boolean,
    torch: Boolean,
    onDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
    pick: (List<String>) -> String? = { it.firstOrNull() },
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentPick by rememberUpdatedState(pick)
    val currentPaused by rememberUpdatedState(paused)
    val currentOnDetected by rememberUpdatedState(onDetected)
    val currentViewfinder by rememberUpdatedState(viewfinder)
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember(formats) {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(formats.first(), *formats.drop(1).toIntArray())
            .build()
        BarcodeScanning.getClient(options)
    }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val stability = remember { StabilityGate() }

    LaunchedEffect(lifecycleOwner, scanner) {
        val provider = ProcessCameraProvider.awaitInstance(context)
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analysis.setAnalyzer(executor) { image ->
            if (currentPaused) {
                image.close()
                return@setAnalyzer
            }
            analyze(image, scanner, previewView, currentViewfinder(), stability, { currentPick(it) }) { value ->
                currentOnDetected(value)
            }
        }
        provider.unbindAll()
        camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
    }

    LaunchedEffect(torch, camera) {
        camera?.cameraControl?.enableTorch(torch)
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
            scanner.close()
            executor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier.pointerInput(camera) {
            detectTapGestures { offset ->
                val cam = camera ?: return@detectTapGestures
                val point = previewView.meteringPointFactory.createPoint(offset.x, offset.y)
                cam.cameraControl.startFocusAndMetering(FocusMeteringAction.Builder(point).build())
            }
        },
    )
}

/** Two identical consecutive reads are required before a value is reported (docs/03 §Scanner). */
internal class StabilityGate {
    private var last: String? = null
    private var reported = false

    /** Returns true when [value] should be reported now. */
    fun accept(value: String): Boolean {
        if (reported) return false
        val stable = value == last
        last = value
        if (stable) reported = true
        return stable
    }

    fun reset() {
        last = null
        reported = false
    }
}

@OptIn(ExperimentalGetImage::class)
private fun analyze(
    image: ImageProxy,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    previewView: PreviewView,
    viewfinder: Rect?,
    gate: StabilityGate,
    pick: (List<String>) -> String?,
    onDetected: (String) -> Unit,
) {
    val media = image.image
    if (media == null) {
        image.close()
        return
    }
    val rotation = image.imageInfo.rotationDegrees
    val input = InputImage.fromMediaImage(media, rotation)
    val uprightWidth = if (rotation == 90 || rotation == 270) image.height else image.width
    val uprightHeight = if (rotation == 90 || rotation == 270) image.width else image.height
    val previewWidth = previewView.width
    val previewHeight = previewView.height
    scanner.process(input)
        .addOnSuccessListener { barcodes ->
            val inside = barcodes.filter { barcode ->
                val value = barcode.rawValue
                !value.isNullOrBlank() &&
                    insideViewfinder(
                        barcode.boundingBox,
                        uprightWidth,
                        uprightHeight,
                        previewWidth,
                        previewHeight,
                        viewfinder,
                    )
            }.mapNotNull { it.rawValue }
            val value = pick(inside) ?: return@addOnSuccessListener
            if (gate.accept(value)) onDetected(value)
        }
        .addOnCompleteListener { image.close() }
}

/** Maps the barcode centre from upright image pixels to preview pixels (FILL_CENTER) and tests the viewfinder. */
internal fun insideViewfinder(
    box: Rect?,
    imageWidth: Int,
    imageHeight: Int,
    previewWidth: Int,
    previewHeight: Int,
    viewfinder: Rect?,
): Boolean {
    if (viewfinder == null || box == null || imageWidth == 0 || imageHeight == 0 || previewWidth == 0 ||
        previewHeight == 0
    ) {
        return true
    }
    val scale = max(previewWidth.toFloat() / imageWidth, previewHeight.toFloat() / imageHeight)
    val offsetX = (imageWidth * scale - previewWidth) / 2f
    val offsetY = (imageHeight * scale - previewHeight) / 2f
    val cx = box.exactCenterX() * scale - offsetX
    val cy = box.exactCenterY() * scale - offsetY
    return viewfinder.contains(cx.toInt(), cy.toInt())
}
