package launcher.launcher.ui.screens.game.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import launcher.launcher.R

@Composable
fun CoinWonDialog(onDismiss:()-> Unit,reward:Int){

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Image(
                painter = painterResource(R.drawable.coin_icon),
                contentDescription = "coin",
                modifier = Modifier.size(50.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.size(10.dp))
            Text(
                text = "You won $reward coin",
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }

}