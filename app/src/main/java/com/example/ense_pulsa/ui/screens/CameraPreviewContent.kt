package com.example.ense_pulsa.ui.screens

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.camera.compose.CameraXViewfinder
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import com.example.ense_pulsa.R
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPreviewScreen(modifier: Modifier = Modifier) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    if (cameraPermissionState.status.isGranted) {
        CameraPreviewContent(modifier = modifier)
    } else {
        val message = if (cameraPermissionState.status.shouldShowRationale) {
            stringResource(R.string.camera_rationale)
        } else {
            stringResource(R.string.camera_required)
        }
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = message)
            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                Text(stringResource(R.string.grant_camera))
            }
        }
    }
}

@Composable
fun CameraPreviewContent(
    viewModel: CameraPreviewViewModel = viewModel(),
    lifecycleOwner: androidx.lifecycle.LifecycleOwner = LocalLifecycleOwner.current,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val surfaceRequest by viewModel.surfaceRequest.collectAsStateWithLifecycle()
    val capturedImage by viewModel.capturedImage.collectAsStateWithLifecycle()
    val overlayRect by viewModel.overlayRect.collectAsStateWithLifecycle()
    val ocrRawText by viewModel.ocrRawText.collectAsStateWithLifecycle()
    val ocrExtractedDigits by viewModel.ocrExtractedDigits.collectAsStateWithLifecycle()
    val isOcrLoading by viewModel.isOcrLoading.collectAsStateWithLifecycle()

    var previewWidth by remember { mutableIntStateOf(0) }
    var previewHeight by remember { mutableIntStateOf(0) }

    LaunchedEffect(lifecycleOwner) {
        viewModel.bindToCamera(context.applicationContext, lifecycleOwner)
    }

    BackHandler(enabled = capturedImage != null) {
        viewModel.clearCapturedImage()
    }

    capturedImage?.let { bitmap ->
        ImagePreviewScreen(
            bitmap = bitmap,
            overlayRect = overlayRect,
            ocrRawText = ocrRawText,
            ocrExtractedDigits = ocrExtractedDigits,
            onBack = { viewModel.clearCapturedImage() }
        )
        if (isOcrLoading) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.padding(top = 16.dp))
                    Text("Processing OCR...", color = Color.White)
                }
            }
        }
    } ?: surfaceRequest?.let { request ->
        Box(modifier = modifier.fillMaxSize()) {
            CameraXViewfinder(
                surfaceRequest = request,
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { size ->
                        previewWidth = size.width
                        previewHeight = size.height
                    }
            )
            if (previewWidth > 0 && previewHeight > 0) {
                OverlayBox(
                    previewWidth = previewWidth,
                    previewHeight = previewHeight,
                    onRectChanged = { viewModel.updateOverlayRect(it) }
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { viewModel.takePhoto(context, previewWidth, previewHeight) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935))
                    )
                }
            }
        }
    }
}
