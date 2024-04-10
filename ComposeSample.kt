mport androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ir.kaaveh.sdpcompose.sdp
import ir.kaaveh.sdpcompose.ssp

@Composable
fun MultiWidgetTourneyComp(
    widgetState: MultiTableWidgetState.Tourney,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = Modifier
            .padding(start = 10.sdp, bottom = 8.sdp)
    ) {
        CenterContent(modifier, widgetState)

        val badgeSize = 8.sdp
        val offSet = -(badgeSize / 2)
        Timer(
            widgetState = widgetState,
            modifier = Modifier
                .align(Alignment.CenterStart)
        )

        if (widgetState.statusText is MultiGameState.EMPTY) {
            widgetState.tourneyTimer?.let {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .offset(y = 8.sdp)
                        .background(color = Color(STATUS_BACKGROUND_COLOR), shape = CircleShape)
                        .padding(horizontal = 8.sdp, vertical = 4.sdp)
                        .align(Alignment.BottomCenter)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_timer),
                        contentDescription = "watch",
                        modifier = Modifier.size(10.sdp)
                    )
                    HorizontalSpacer(width = 4.sdp)
                    Text(
                        text = it,
                        style = TextStyle(
                            color = Color(0XFF000000),
                            fontSize = 10.ssp,
                            fontFamily = DefaultFonts.ROBOTO_FAMILY,
                            fontWeight = FontWeight.W700
                        )
                    )
                }
            }
        } else {
            Text(
                text = widgetState.statusText.uiText,
                fontSize = 8.ssp,
                style = TextStyle(
                    color = Color(0XFF000000),
                    fontSize = 10.ssp,
                    fontFamily = DefaultFonts.ROBOTO_FAMILY,
                    fontWeight = FontWeight.W700
                ),
                modifier = Modifier
                    .offset(y = 8.sdp)
                    .background(color = Color(STATUS_BACKGROUND_COLOR), shape = CircleShape)
                    .padding(horizontal = 8.sdp, vertical = 4.sdp)
                    .align(Alignment.BottomCenter)
            )
        }

    }
}

@Composable
private fun CenterContent(
    modifier: Modifier,
    widgetState: MultiTableWidgetState.Tourney
) {

    val time = widgetState.timer?.toIntOrNull()

    // See if needed optimizations using derived state.
    val borderColor: Color = if (time == null) {
        Color(BORDER_COLOR)
    } else if (widgetState.statusText is MultiGameState.YOUR_TURN) {
        if (time - 15 > 0) {
            // normal time
            if (time % 2 == 0) {
                Color(BORDER_COLOR_NORMAL_TIME)
            } else {
                Color(BORDER_COLOR)
            }
        } else {
            // extra time
            if (time % 2 == 0) {
                Color(BORDER_COLOR_EXTRA_TIME)
            } else {
                Color(BORDER_COLOR)
            }
        }
    } else {
        Color(BORDER_COLOR)
    }

    val backgroundColorBrush = if (widgetState.statusText is MultiGameState.YOUR_TURN) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF04C250),
                Color(0xFF003817),
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFF2E2E2E),
                Color(0xFF1B1B1B),
            )
        )
    }

    Column(
        modifier = modifier
            .width(100.sdp)
            .background(
                brush = backgroundColorBrush,
                shape = RoundedCornerShape(percent = 100)
            )
            .border(
                width = 1.sdp,
                color = borderColor,
                shape = RoundedCornerShape(percent = 50)
            )
            .padding(start = 20.sdp, end = 20.sdp, top = 4.sdp, bottom = 7.sdp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlayersSeatsWidget(players = widgetState.players, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.sdp))
        Text(
            text = widgetState.tourneyName,
            fontSize = 8.ssp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = Color(0xFF0AFA39),
                fontFamily = DefaultFonts.ROBOTO_FAMILY,
                fontWeight = FontWeight.W700
            )
        )
        Text(
            text = "Tourney",
            fontSize = 8.ssp,
            style = TextStyle(
                color = Color(0XFFFFFFFF),
                fontFamily = DefaultFonts.ROBOTO_FAMILY,
                fontWeight = FontWeight.W700
            )
        )
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .size(10.sdp)
                .background(
                    color = if (widgetState.statusText is MultiGameState.YOUR_TURN)
                        Color(SEAT_TURN)
                    else
                        Color(SEAT_OCCUPIED),
                    CircleShape
                )
        )
    }
}

@Composable
private fun Timer(widgetState: MultiTableWidgetState.Tourney, modifier: Modifier = Modifier) {
    val it = widgetState.timer?.toIntOrNull()

    if (it != null && widgetState.statusText is MultiGameState.YOUR_TURN) {
        val backgroundColor: Color =
            if (true && it <= 15) {
                Color(TIMER_BACKGROUND_COLOR_EXTRA_TIME)
            } else {
                Color(TIMER_BACKGROUND_COLOR_NORMAL_TIME)
            }
        Text(
            text = it.toString(),
            modifier = modifier
                .offset(x = -5.sdp, y = 0.dp)
                .drawBehind {
                    drawCircle(
                        color = backgroundColor,
                        radius = this.size.maxDimension / 1.3f
                    )
                },
            style = TextStyle(
                color = Color.Black,
                fontFamily = DefaultFonts.ROBOTO_FAMILY,
                fontSize = 10.ssp,
                fontWeight = FontWeight.W700
            ),
        )
    } else {
        Icon(
            painter = painterResource(id = R.drawable.ic_trophy),
            contentDescription = "Tourney award",
            tint = Color.Unspecified,
            modifier = modifier
                .offset(x = -8.sdp, y = 0.dp)
                .size(24.sdp)
                .background(color = Color(TIMER_BACKGROUND_COLOR), shape = CircleShape)
                .padding(6.sdp)
            // .drawBehind {
            //     drawCircle(
            //         color = Color(0XFFFFF500),
            //         radius = size.minDimension / 2.0f,
            //     )
            // },
        )
    }
}

@Preview
@Composable
private fun MultiTableTourneyWidgetCompPreview() {
    MultiWidgetTourneyComp(
        widgetState = MultiTableWidgetState.Tourney(
            gameStateManagerKey = "1234",
            statusText = MultiGameState.EMPTY,
            // statusText = MultiGameState.YOUR_TURN(23),
            // statusText = MultiGameState.WON,
            players = List(6) {
                MultiWidgetPlayer(
                    it,
                    if (it % 2 == 0) PlayerWidgetState.OCCUPIED else PlayerWidgetState.EMPTY,
                    isTurn = it % 2 == 0,
                    userName = "hkstaging"
                )
            },
            timer = "123",
            // timer = null,
            tourneyTimer = "2h 4m",
            tourneyName = "Sunday kings"
        )
    )
}