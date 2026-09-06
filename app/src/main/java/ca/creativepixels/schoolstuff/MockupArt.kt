package ca.creativepixels.schoolstuff

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

enum class MockupAsset {
    BACKPACK, CAMERA, BOOKS, PIZZA, FOLDER, SHOES, SHIRT, SANDWICH, PALETTE, DOCUMENT, STAR
}

@Composable
fun MockupArtImage(
    asset: MockupAsset,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit
) {
    Image(
        painter = painterResource(id = asset.drawableRes()),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}

private fun MockupAsset.drawableRes(): Int = when (this) {
    MockupAsset.BACKPACK -> R.drawable.asset_backpack
    MockupAsset.CAMERA -> R.drawable.asset_camera
    MockupAsset.BOOKS -> R.drawable.asset_books
    MockupAsset.PIZZA -> R.drawable.asset_pizza
    MockupAsset.FOLDER -> R.drawable.asset_folder
    MockupAsset.SHOES -> R.drawable.asset_shoes
    MockupAsset.SHIRT -> R.drawable.asset_shirt
    MockupAsset.SANDWICH -> R.drawable.asset_sandwich
    MockupAsset.PALETTE -> R.drawable.asset_palette
    MockupAsset.DOCUMENT -> R.drawable.asset_document
    MockupAsset.STAR -> R.drawable.asset_folder
}
