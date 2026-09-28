package com.example.jarvisai.ui.mascot

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// =========================================================================
// OFFICIAL JARVIS 3.0 MASCOT PALETTE
// =========================================================================
private val MascotCyan = Color(0xFF00E5FF)       // Azul principal
private val MascotBlue = Color(0xFF0091FF)       // Azul secundario
private val MascotSilver = Color(0xFFE6F1FF)     // Blanco / Plata
private val MascotSilverDark = Color(0xFFC0D2E8) // Sombra metálica
private val MascotBlack = Color(0xFF0A0F14)      // Pantalla visor negro
private val MascotJointGray = Color(0xFF2A2F3A)  // Articulaciones mecánicas
private val MascotWarningRed = Color(0xFFFF3D57) // Advertencia / enojo sutil
private val MascotSuccessGreen = Color(0xFF00E676)// Éxito confirmación

/**
 * JARVIS 3.0 Official Visual Mascot Component.
 * Fully layered procedural vector rendering with natural animations,
 * reactive expressions, audio-reactive listening waves, thinking halo,
 * action holographic pads, and speech modulation.
 */
@Composable
fun JarvisMascot(
    state: MascotState = MascotState.IDLE,
    expression: MascotExpression = MascotExpression.SERENO,
    size: Dp = 220.dp,
    audioAmplitude: Float = 0f,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val smoothAmplitude by animateFloatAsState(
        targetValue = audioAmplitude.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "mascot_amplitude"
    )

    // Infinite transitions for organic life-like motions
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_mascot_anim")

    // Organic Breathing (Gentle 2.8s floating cycle)
    val breathProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == MascotState.SLEEPING) 4000 else 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "breath_progress"
    )

    // Occasional blinking animation (closes briefly every ~3.5 seconds)
    val blinkPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "blink_phase"
    )
    val isBlinking = (blinkPhase > 0.94f) && (state != MascotState.SLEEPING) && (expression != MascotExpression.DORMIDO)

    // Thinking Halo Rotation & Pulsing
    val haloRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_rotation"
    )

    // Speech rhythm wave oscillation
    val speechWave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "speech_wave"
    )

    // Action data pulse
    val actionPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "action_pulse"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = this.size.width
            val h = this.size.height

            // Global floating / breathing translation (subtle vertical bob)
            val breathOffset = sin(breathProgress) * (h * 0.018f)

            // Dynamic Head Tilt based on state / expression
            val headTiltDegrees = when (expression) {
                MascotExpression.CURIOSO -> 7.5f
                MascotExpression.PENSANDO -> -5f
                MascotExpression.FELIZ -> 3.5f
                MascotExpression.SORPRENDIDO -> -3f
                MascotExpression.TRISTE -> -4f
                MascotExpression.DORMIDO -> 6f
                else -> if (state == MascotState.THINKING) -4.5f else 0f
            }

            // 1. Base Shadow and Ground Glow Aura
            drawMascotGroundShadow(w, h, breathOffset, state)

            // 2. Legs and Feet
            drawMascotLegs(w, h, breathOffset, state)

            // 3. Body / Torso & Chest Emblem
            drawMascotTorso(w, h, breathOffset, state)

            // 4. Arms & Hands (Animated per posture)
            drawMascotArms(w, h, breathOffset, state, expression, speechWave, actionPulse)

            // 5. Head, Visor, Headphones & Face Expression
            drawMascotHeadAndFace(
                w = w,
                h = h,
                breathOffset = breathOffset,
                tiltDegrees = headTiltDegrees,
                state = state,
                expression = expression,
                isBlinking = isBlinking,
                amplitude = smoothAmplitude,
                haloRotation = haloRotation,
                speechWave = speechWave,
                actionPulse = actionPulse
            )

            // 6. Holographic Badges & State Overlays ('?', '!', Zzz, Success ✓, Error ⚠️, Action Tablet)
            drawMascotStateOverlays(
                w = w,
                h = h,
                breathOffset = breathOffset,
                tiltDegrees = headTiltDegrees,
                state = state,
                expression = expression,
                breathProgress = breathProgress,
                actionPulse = actionPulse
            )
        }
    }
}

// =========================================================================
// 1. GROUND SHADOW & AMBIENT GLOW
// =========================================================================
private fun DrawScope.drawMascotGroundShadow(w: Float, h: Float, breathOffset: Float, state: MascotState) {
    val shadowY = h * 0.94f
    val shadowWidth = w * 0.52f * (1f - (breathOffset / (h * 0.2f)))
    val shadowHeight = h * 0.075f

    // Soft cyan ambient ground pool
    val auraColor = when (state) {
        MascotState.LIVE, MascotState.SPEAKING -> MascotCyan.copy(alpha = 0.35f)
        MascotState.LISTENING -> MascotCyan.copy(alpha = 0.45f)
        MascotState.THINKING -> MascotBlue.copy(alpha = 0.35f)
        MascotState.EXECUTING_ACTION -> MascotCyan.copy(alpha = 0.5f)
        MascotState.ERROR -> MascotWarningRed.copy(alpha = 0.3f)
        MascotState.SUCCESS -> MascotSuccessGreen.copy(alpha = 0.45f)
        MascotState.SLEEPING -> MascotCyan.copy(alpha = 0.12f)
        else -> MascotCyan.copy(alpha = 0.22f)
    }

    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(auraColor, Color.Transparent),
            center = Offset(w * 0.5f, shadowY),
            radius = shadowWidth * 0.85f
        ),
        topLeft = Offset(w * 0.5f - shadowWidth * 0.65f, shadowY - shadowHeight * 0.5f),
        size = Size(shadowWidth * 1.3f, shadowHeight * 1.6f)
    )

    // Deep contact core shadow
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x99000000), Color.Transparent),
            center = Offset(w * 0.5f, shadowY),
            radius = shadowWidth * 0.5f
        ),
        topLeft = Offset(w * 0.5f - shadowWidth * 0.5f, shadowY - shadowHeight * 0.45f),
        size = Size(shadowWidth, shadowHeight * 0.9f)
    )
}

// =========================================================================
// 2. LEGS & FEET
// =========================================================================
private fun DrawScope.drawMascotLegs(w: Float, h: Float, breathOffset: Float, state: MascotState) {
    val leftLegX = w * 0.41f
    val rightLegX = w * 0.59f
    val legY = h * 0.81f + (breathOffset * 0.3f)
    val footWidth = w * 0.125f
    val footHeight = h * 0.08f

    // Left and Right Boots / Foot Capsules
    listOf(leftLegX, rightLegX).forEach { cx ->
        // Black ankle joint
        drawRoundRect(
            color = MascotJointGray,
            topLeft = Offset(cx - footWidth * 0.35f, legY - h * 0.02f),
            size = Size(footWidth * 0.7f, h * 0.04f),
            cornerRadius = CornerRadius(4f, 4f)
        )

        // White metallic shoe capsule
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(MascotSilver, MascotSilverDark),
                startY = legY,
                endY = legY + footHeight
            ),
            topLeft = Offset(cx - footWidth * 0.5f, legY),
            size = Size(footWidth, footHeight),
            cornerRadius = CornerRadius(footWidth * 0.45f, footWidth * 0.45f)
        )

        // Subtle cyan sole trim
        drawRoundRect(
            color = MascotCyan.copy(alpha = 0.7f),
            topLeft = Offset(cx - footWidth * 0.42f, legY + footHeight * 0.75f),
            size = Size(footWidth * 0.84f, footHeight * 0.18f),
            cornerRadius = CornerRadius(2f, 2f)
        )
    }
}

// =========================================================================
// 3. BODY & TORSO
// =========================================================================
private fun DrawScope.drawMascotTorso(w: Float, h: Float, breathOffset: Float, state: MascotState) {
    val torsoCenterX = w * 0.5f
    val torsoTopY = h * 0.58f + breathOffset
    val torsoWidth = w * 0.34f
    val torsoHeight = h * 0.25f

    // Neck joint connection
    drawRoundRect(
        color = MascotJointGray,
        topLeft = Offset(torsoCenterX - torsoWidth * 0.22f, torsoTopY - h * 0.035f),
        size = Size(torsoWidth * 0.44f, h * 0.055f),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Main Torso Shell (Metallic Pearl White / Silver)
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(MascotSilver, Color(0xFFD6E4F0), MascotSilverDark),
            startY = torsoTopY,
            endY = torsoTopY + torsoHeight
        ),
        topLeft = Offset(torsoCenterX - torsoWidth * 0.5f, torsoTopY),
        size = Size(torsoWidth, torsoHeight),
        cornerRadius = CornerRadius(torsoWidth * 0.38f, torsoWidth * 0.38f)
    )

    // Side tech panel cuts
    val sideCutWidth = torsoWidth * 0.12f
    listOf(torsoCenterX - torsoWidth * 0.44f, torsoCenterX + torsoWidth * 0.32f).forEach { cutX ->
        drawRoundRect(
            color = MascotJointGray.copy(alpha = 0.5f),
            topLeft = Offset(cutX, torsoTopY + torsoHeight * 0.22f),
            size = Size(sideCutWidth, torsoHeight * 0.5f),
            cornerRadius = CornerRadius(4f, 4f)
        )
    }

    // Chest Arc Reactor / JARVIS 'A' Emblem
    val emblemCenterY = torsoTopY + torsoHeight * 0.38f
    val emblemRadius = torsoWidth * 0.16f

    // Glowing emblem background
    drawCircle(
        color = MascotBlack,
        radius = emblemRadius,
        center = Offset(torsoCenterX, emblemCenterY)
    )
    drawCircle(
        color = MascotCyan.copy(alpha = 0.35f),
        radius = emblemRadius * 1.35f,
        center = Offset(torsoCenterX, emblemCenterY)
    )

    // Inscribed cyan glowing triangle 'A'
    val path = Path().apply {
        moveTo(torsoCenterX, emblemCenterY - emblemRadius * 0.65f)
        lineTo(torsoCenterX + emblemRadius * 0.6f, emblemCenterY + emblemRadius * 0.5f)
        lineTo(torsoCenterX - emblemRadius * 0.6f, emblemCenterY + emblemRadius * 0.5f)
        close()
    }
    drawPath(path = path, color = MascotCyan, style = Stroke(width = 2.5f))

    // Tiny center reactor dot
    drawCircle(
        color = Color.White,
        radius = emblemRadius * 0.25f,
        center = Offset(torsoCenterX, emblemCenterY + emblemRadius * 0.05f)
    )

    // Bottom Belt / Waist trim
    drawRoundRect(
        color = MascotBlack.copy(alpha = 0.85f),
        topLeft = Offset(torsoCenterX - torsoWidth * 0.38f, torsoTopY + torsoHeight * 0.82f),
        size = Size(torsoWidth * 0.76f, torsoHeight * 0.12f),
        cornerRadius = CornerRadius(3f, 3f)
    )
}

// =========================================================================
// 4. ARMS & HANDS
// =========================================================================
private fun DrawScope.drawMascotArms(
    w: Float,
    h: Float,
    breathOffset: Float,
    state: MascotState,
    expression: MascotExpression,
    speechWave: Float,
    actionPulse: Float
) {
    val torsoY = h * 0.58f + breathOffset
    val leftShoulderX = w * 0.33f
    val rightShoulderX = w * 0.67f
    val armWidth = w * 0.09f
    val armLength = h * 0.17f

    when {
        // High celebration / Excited / Surprised: Hands up in the air!
        expression == MascotExpression.EMOCIONADO || expression == MascotExpression.SORPRENDIDO || state == MascotState.SUCCESS -> {
            drawUpwardArm(leftShoulderX, torsoY, armWidth, armLength, isLeft = true)
            drawUpwardArm(rightShoulderX, torsoY, armWidth, armLength, isLeft = false)
        }
        // Thinking posture: Right hand to chin
        expression == MascotExpression.PENSANDO || state == MascotState.THINKING -> {
            drawNormalArm(leftShoulderX, torsoY, armWidth, armLength, isLeft = true, angleDeg = 15f)
            drawHandToChinArm(rightShoulderX, torsoY, armWidth, armLength, w, h)
        }
        // Executing action: Hands holding holographic pad forward
        state == MascotState.EXECUTING_ACTION -> {
            drawForwardArm(leftShoulderX, torsoY, armWidth, armLength, isLeft = true)
            drawForwardArm(rightShoulderX, torsoY, armWidth, armLength, isLeft = false)
        }
        // Speaking: Subtle organic gesticulation with speech cadence
        state == MascotState.SPEAKING -> {
            val leftSway = sin(speechWave) * 12f
            val rightSway = cos(speechWave) * 14f
            drawNormalArm(leftShoulderX, torsoY, armWidth, armLength, isLeft = true, angleDeg = 20f + leftSway)
            drawNormalArm(rightShoulderX, torsoY, armWidth, armLength, isLeft = false, angleDeg = -22f - rightSway)
        }
        // Curious: One hand slightly raised
        expression == MascotExpression.CURIOSO -> {
            drawNormalArm(leftShoulderX, torsoY, armWidth, armLength, isLeft = true, angleDeg = 12f)
            drawNormalArm(rightShoulderX, torsoY, armWidth, armLength, isLeft = false, angleDeg = -42f)
        }
        // Default / Relaxed: resting naturally at sides
        else -> {
            drawNormalArm(leftShoulderX, torsoY, armWidth, armLength, isLeft = true, angleDeg = 15f)
            drawNormalArm(rightShoulderX, torsoY, armWidth, armLength, isLeft = false, angleDeg = -15f)
        }
    }
}

private fun DrawScope.drawNormalArm(shoulderX: Float, shoulderY: Float, armW: Float, armL: Float, isLeft: Boolean, angleDeg: Float) {
    rotate(angleDeg, pivot = Offset(shoulderX, shoulderY)) {
        // Shoulder Joint
        drawCircle(color = MascotJointGray, radius = armW * 0.55f, center = Offset(shoulderX, shoulderY))

        // Arm Capsule
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(MascotSilver, MascotSilverDark), startY = shoulderY, endY = shoulderY + armL),
            topLeft = Offset(shoulderX - armW * 0.5f, shoulderY),
            size = Size(armW, armL),
            cornerRadius = CornerRadius(armW * 0.45f, armW * 0.45f)
        )

        // Hand Glove (White sphere with dark cuff)
        val handY = shoulderY + armL
        drawCircle(color = MascotJointGray, radius = armW * 0.52f, center = Offset(shoulderX, handY))
        drawCircle(color = MascotSilver, radius = armW * 0.5f, center = Offset(shoulderX, handY + armW * 0.2f))
    }
}

private fun DrawScope.drawUpwardArm(shoulderX: Float, shoulderY: Float, armW: Float, armL: Float, isLeft: Boolean) {
    val angle = if (isLeft) -135f else 135f
    rotate(angle, pivot = Offset(shoulderX, shoulderY)) {
        drawCircle(color = MascotJointGray, radius = armW * 0.55f, center = Offset(shoulderX, shoulderY))
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(MascotSilver, MascotSilverDark)),
            topLeft = Offset(shoulderX - armW * 0.5f, shoulderY),
            size = Size(armW, armL),
            cornerRadius = CornerRadius(armW * 0.45f, armW * 0.45f)
        )
        drawCircle(color = MascotSilver, radius = armW * 0.52f, center = Offset(shoulderX, shoulderY + armL))
    }
}

private fun DrawScope.drawForwardArm(shoulderX: Float, shoulderY: Float, armW: Float, armL: Float, isLeft: Boolean) {
    val angle = if (isLeft) 45f else -45f
    rotate(angle, pivot = Offset(shoulderX, shoulderY)) {
        drawCircle(color = MascotJointGray, radius = armW * 0.55f, center = Offset(shoulderX, shoulderY))
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(MascotSilver, MascotSilverDark)),
            topLeft = Offset(shoulderX - armW * 0.5f, shoulderY),
            size = Size(armW, armL * 0.85f),
            cornerRadius = CornerRadius(armW * 0.45f, armW * 0.45f)
        )
        drawCircle(color = MascotSilver, radius = armW * 0.52f, center = Offset(shoulderX, shoulderY + armL * 0.85f))
    }
}

private fun DrawScope.drawHandToChinArm(shoulderX: Float, shoulderY: Float, armW: Float, armL: Float, w: Float, h: Float) {
    rotate(-70f, pivot = Offset(shoulderX, shoulderY)) {
        drawCircle(color = MascotJointGray, radius = armW * 0.55f, center = Offset(shoulderX, shoulderY))
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(MascotSilver, MascotSilverDark)),
            topLeft = Offset(shoulderX - armW * 0.5f, shoulderY),
            size = Size(armW, armL * 0.88f),
            cornerRadius = CornerRadius(armW * 0.45f, armW * 0.45f)
        )
        drawCircle(color = MascotSilver, radius = armW * 0.55f, center = Offset(shoulderX, shoulderY + armL * 0.88f))
    }
}

// =========================================================================
// 5. HEAD, VISOR, HEADPHONES & FACE
// =========================================================================
private fun DrawScope.drawMascotHeadAndFace(
    w: Float,
    h: Float,
    breathOffset: Float,
    tiltDegrees: Float,
    state: MascotState,
    expression: MascotExpression,
    isBlinking: Boolean,
    amplitude: Float,
    haloRotation: Float,
    speechWave: Float,
    actionPulse: Float
) {
    val headCenterX = w * 0.5f
    val headCenterY = h * 0.36f + breathOffset
    val headWidth = w * 0.64f
    val headHeight = h * 0.44f

    rotate(tiltDegrees, pivot = Offset(headCenterX, headCenterY)) {

        // Head Aura Glow (Reacts dynamically to states)
        val headGlowColor = when (state) {
            MascotState.LIVE, MascotState.SPEAKING -> MascotCyan.copy(alpha = 0.28f)
            MascotState.LISTENING -> MascotCyan.copy(alpha = 0.38f + amplitude * 0.3f)
            MascotState.THINKING -> MascotBlue.copy(alpha = 0.32f)
            MascotState.EXECUTING_ACTION -> MascotCyan.copy(alpha = 0.4f)
            MascotState.ERROR -> MascotWarningRed.copy(alpha = 0.25f)
            MascotState.SUCCESS -> MascotSuccessGreen.copy(alpha = 0.35f)
            else -> MascotCyan.copy(alpha = 0.15f)
        }

        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(headGlowColor, Color.Transparent),
                center = Offset(headCenterX, headCenterY),
                radius = headWidth * 0.72f
            ),
            topLeft = Offset(headCenterX - headWidth * 0.65f, headCenterY - headHeight * 0.65f),
            size = Size(headWidth * 1.3f, headHeight * 1.3f)
        )

        // Thinking Halo Ring (when thinking / processing)
        if (state == MascotState.THINKING || expression == MascotExpression.PENSANDO) {
            drawThinkingHalo(headCenterX, headCenterY - headHeight * 0.52f, headWidth * 0.45f, haloRotation)
        }

        // Headphones & Soundwave Arcs (Left & Right Earmuffs)
        val earOffset = headWidth * 0.48f
        val earRadius = headHeight * 0.24f
        drawMascotEarmuffs(
            leftX = headCenterX - earOffset,
            rightX = headCenterX + earOffset,
            y = headCenterY,
            radius = earRadius,
            state = state,
            amplitude = amplitude
        )

        // Main Helmet Metallic Shell (Glossy white/silver pebble shape)
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFFFFF), MascotSilver, MascotSilverDark),
                startY = headCenterY - headHeight * 0.5f,
                endY = headCenterY + headHeight * 0.5f
            ),
            topLeft = Offset(headCenterX - headWidth * 0.5f, headCenterY - headHeight * 0.5f),
            size = Size(headWidth, headHeight),
            cornerRadius = CornerRadius(headWidth * 0.46f, headHeight * 0.48f)
        )

        // Helmet Specular Top Gloss Highlight
        drawOval(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.7f), Color.Transparent),
                startY = headCenterY - headHeight * 0.46f,
                endY = headCenterY - headHeight * 0.2f
            ),
            topLeft = Offset(headCenterX - headWidth * 0.28f, headCenterY - headHeight * 0.48f),
            size = Size(headWidth * 0.56f, headHeight * 0.22f)
        )

        // Forehead JARVIS Cyan Triangle Badge
        val badgeY = headCenterY - headHeight * 0.38f
        val badgeSize = headWidth * 0.055f
        val badgePath = Path().apply {
            moveTo(headCenterX, badgeY - badgeSize)
            lineTo(headCenterX + badgeSize * 0.85f, badgeY + badgeSize * 0.6f)
            lineTo(headCenterX - badgeSize * 0.85f, badgeY + badgeSize * 0.6f)
            close()
        }
        drawPath(badgePath, color = MascotCyan, style = Fill)

        // Visor Screen Frame (Black Obsidian Glass Bezel)
        val visorWidth = headWidth * 0.76f
        val visorHeight = headHeight * 0.68f
        val visorTop = headCenterY - visorHeight * 0.46f
        val visorLeft = headCenterX - visorWidth * 0.5f

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0F1824), MascotBlack, Color(0xFF070B10)),
                startY = visorTop,
                endY = visorTop + visorHeight
            ),
            topLeft = Offset(visorLeft, visorTop),
            size = Size(visorWidth, visorHeight),
            cornerRadius = CornerRadius(visorWidth * 0.38f, visorHeight * 0.44f)
        )

        // Visor Neon Blue Rim Border
        drawRoundRect(
            color = MascotCyan.copy(alpha = 0.25f),
            topLeft = Offset(visorLeft, visorTop),
            size = Size(visorWidth, visorHeight),
            cornerRadius = CornerRadius(visorWidth * 0.38f, visorHeight * 0.44f),
            style = Stroke(width = 1.5f)
        )

        // Visor Curved Glass Specular Arc
        val glassArcPath = Path().apply {
            moveTo(visorLeft + visorWidth * 0.15f, visorTop + visorHeight * 0.12f)
            quadraticTo(
                headCenterX, visorTop + visorHeight * 0.08f,
                visorLeft + visorWidth * 0.85f, visorTop + visorHeight * 0.12f
            )
        }
        drawPath(glassArcPath, color = Color.White.copy(alpha = 0.25f), style = Stroke(width = 2.5f, cap = StrokeCap.Round))

        // Eyes & Facial Expressions (Core emotional delivery)
        drawMascotEyes(
            centerX = headCenterX,
            centerY = headCenterY + headHeight * 0.02f,
            visorW = visorWidth,
            visorH = visorHeight,
            expression = expression,
            isBlinking = isBlinking,
            state = state
        )

        // Speech Audio Waves in Visor (when SPEAKING)
        if (state == MascotState.SPEAKING) {
            drawVisorSpeechWaves(headCenterX, headCenterY + visorHeight * 0.3f, visorWidth * 0.36f, speechWave)
        }
    }
}

// =========================================================================
// 5B. EARMUFFS & LISTENING ACOUSTIC WAVES
// =========================================================================
private fun DrawScope.drawMascotEarmuffs(leftX: Float, rightX: Float, y: Float, radius: Float, state: MascotState, amplitude: Float) {
    listOf(leftX, rightX).forEachIndexed { index, cx ->
        val isLeft = index == 0

        // Earmuff body (Metallic dark gray cylinder)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(MascotSilver, MascotJointGray),
                center = Offset(cx, y),
                radius = radius
            ),
            radius = radius,
            center = Offset(cx, y)
        )

        // Inner glowing electric ring
        drawCircle(
            color = MascotBlack,
            radius = radius * 0.65f,
            center = Offset(cx, y)
        )
        drawCircle(
            color = MascotCyan,
            radius = radius * 0.45f,
            center = Offset(cx, y),
            style = Stroke(width = 2.5f)
        )
        drawCircle(
            color = Color.White,
            radius = radius * 0.18f,
            center = Offset(cx, y)
        )

        // Soundwave Arcs (When LISTENING: Acoustic ripples expand)
        if (state == MascotState.LISTENING || state == MascotState.LIVE) {
            val waveColor = MascotCyan.copy(alpha = 0.75f)
            val baseSpread = radius * (1.3f + amplitude * 0.6f)

            for (ring in 1..2) {
                val ringRadius = baseSpread + (ring * radius * 0.5f)
                val arcStartAngle = if (isLeft) 110f else -70f
                val sweep = 140f

                drawArc(
                    color = waveColor.copy(alpha = (0.8f - ring * 0.3f).coerceAtLeast(0.1f)),
                    startAngle = arcStartAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(cx - ringRadius, y - ringRadius),
                    size = Size(ringRadius * 2f, ringRadius * 2f),
                    style = Stroke(width = 2.8f - ring * 0.6f, cap = StrokeCap.Round)
                )
            }
        }
    }
}

// =========================================================================
// 5C. THINKING HALO
// =========================================================================
private fun DrawScope.drawThinkingHalo(cx: Float, cy: Float, radius: Float, rotation: Float) {
    rotate(rotation, pivot = Offset(cx, cy)) {
        drawOval(
            brush = Brush.sweepGradient(
                listOf(MascotCyan, MascotBlue, Color.Transparent, MascotCyan)
            ),
            topLeft = Offset(cx - radius, cy - radius * 0.35f),
            size = Size(radius * 2f, radius * 0.7f),
            style = Stroke(width = 3.5f)
        )
        // Radiant node dots
        drawCircle(color = Color.White, radius = 3.5f, center = Offset(cx + radius * 0.9f, cy))
        drawCircle(color = MascotCyan, radius = 3f, center = Offset(cx - radius * 0.9f, cy))
    }
}

// =========================================================================
// 5D. EYES & 10 OFFICIAL EXPRESSIONS
// =========================================================================
private fun DrawScope.drawMascotEyes(
    centerX: Float,
    centerY: Float,
    visorW: Float,
    visorH: Float,
    expression: MascotExpression,
    isBlinking: Boolean,
    state: MascotState
) {
    val eyeSpacing = visorW * 0.22f
    val eyeWidth = visorW * 0.165f
    val eyeHeight = visorH * 0.28f

    val leftEyeCenter = Offset(centerX - eyeSpacing, centerY)
    val rightEyeCenter = Offset(centerX + eyeSpacing, centerY)

    // In natural blinking, eyes flatten into a subtle glowing slit
    if (isBlinking) {
        listOf(leftEyeCenter, rightEyeCenter).forEach { center ->
            drawLine(
                color = MascotCyan,
                start = Offset(center.x - eyeWidth * 0.5f, center.y),
                end = Offset(center.x + eyeWidth * 0.5f, center.y),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }
        return
    }

    when (expression) {
        // 1. FELIZ: Happy upward curved smiling arcs (⌒ ⌒)
        MascotExpression.FELIZ -> {
            listOf(leftEyeCenter, rightEyeCenter).forEach { center ->
                drawHappyEyeArc(center, eyeWidth, eyeHeight)
            }
        }

        // 2. CURIOSO: Wide round curious circles (⊙ ⊙)
        MascotExpression.CURIOSO -> {
            listOf(leftEyeCenter, rightEyeCenter).forEach { center ->
                drawRoundEyeCircle(center, eyeWidth * 0.5f, isDilated = true)
            }
        }

        // 3. PENSANDO: Eyes directed upward and right
        MascotExpression.PENSANDO -> {
            listOf(leftEyeCenter, rightEyeCenter).forEach { center ->
                val shiftedCenter = Offset(center.x + eyeWidth * 0.15f, center.y - eyeHeight * 0.15f)
                drawRoundEyeCircle(shiftedCenter, eyeWidth * 0.44f, isDilated = false)
            }
        }

        // 4. SORPRENDIDO: Very open dilated circular eyes (◎ ◎)
        MascotExpression.SORPRENDIDO -> {
            listOf(leftEyeCenter, rightEyeCenter).forEach { center ->
                drawSurprisedEye(center, eyeWidth * 0.58f)
            }
        }

        // 5. TRISTE: Slanted downward curved eyes (\ /)
        MascotExpression.TRISTE -> {
            drawSadEye(leftEyeCenter, eyeWidth, eyeHeight, isLeft = true)
            drawSadEye(rightEyeCenter, eyeWidth, eyeHeight, isLeft = false)
        }

        // 6. ENOJADO: Slanted fierce angular eyes (/ \) with red accent
        MascotExpression.ENOJADO -> {
            drawAngryEye(leftEyeCenter, eyeWidth, eyeHeight, isLeft = true)
            drawAngryEye(rightEyeCenter, eyeWidth, eyeHeight, isLeft = false)
        }

        // 7. ASOMBRADO: Big sparkling glowing rings
        MascotExpression.ASOMBRADO -> {
            listOf(leftEyeCenter, rightEyeCenter).forEach { center ->
                drawSurprisedEye(center, eyeWidth * 0.52f)
            }
        }

        // 8. SERENO: Gentle calm horizontal curved lines (- -)
        MascotExpression.SERENO -> {
            listOf(leftEyeCenter, rightEyeCenter).forEach { center ->
                drawSereneEye(center, eyeWidth)
            }
        }

        // 9. DORMIDO: Downward soft closed curved eyes
        MascotExpression.DORMIDO -> {
            listOf(leftEyeCenter, rightEyeCenter).forEach { center ->
                drawSleepingEyeArc(center, eyeWidth, eyeHeight)
            }
        }

        // 10. EMOCIONADO: Glowing 4-pointed radiant stars (✦ ✦)
        MascotExpression.EMOCIONADO -> {
            listOf(leftEyeCenter, rightEyeCenter).forEach { center ->
                drawStarEye(center, eyeWidth * 0.55f)
            }
        }
    }
}

// -------------------------------------------------------------------------
// Eye Geometry Helpers
// -------------------------------------------------------------------------
private fun DrawScope.drawHappyEyeArc(center: Offset, w: Float, h: Float) {
    val path = Path().apply {
        moveTo(center.x - w * 0.5f, center.y + h * 0.2f)
        quadraticTo(center.x, center.y - h * 0.55f, center.x + w * 0.5f, center.y + h * 0.2f)
    }
    // Neon Cyan glow and core stroke
    drawPath(path, color = MascotCyan.copy(alpha = 0.45f), style = Stroke(width = 8f, cap = StrokeCap.Round))
    drawPath(path, color = MascotCyan, style = Stroke(width = 4.5f, cap = StrokeCap.Round))
    drawPath(path, color = Color.White, style = Stroke(width = 1.8f, cap = StrokeCap.Round))
}

private fun DrawScope.drawRoundEyeCircle(center: Offset, radius: Float, isDilated: Boolean) {
    // Outer electric glow
    drawCircle(
        color = MascotCyan.copy(alpha = 0.35f),
        radius = radius * 1.35f,
        center = center
    )
    // Core Cyan Pupil
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, MascotCyan, MascotBlue),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
    // White eye specular highlight reflection dot
    val highlightOffset = Offset(center.x - radius * 0.32f, center.y - radius * 0.32f)
    drawCircle(color = Color.White, radius = radius * 0.28f, center = highlightOffset)
}

private fun DrawScope.drawSurprisedEye(center: Offset, radius: Float) {
    drawCircle(color = MascotCyan.copy(alpha = 0.4f), radius = radius * 1.4f, center = center)
    drawCircle(color = MascotCyan, radius = radius, center = center, style = Stroke(width = 4f))
    drawCircle(color = Color.White, radius = radius * 0.45f, center = center)
}

private fun DrawScope.drawSadEye(center: Offset, w: Float, h: Float, isLeft: Boolean) {
    val path = Path().apply {
        if (isLeft) {
            moveTo(center.x - w * 0.5f, center.y + h * 0.3f)
            quadraticTo(center.x, center.y - h * 0.1f, center.x + w * 0.5f, center.y - h * 0.3f)
        } else {
            moveTo(center.x - w * 0.5f, center.y - h * 0.3f)
            quadraticTo(center.x, center.y - h * 0.1f, center.x + w * 0.5f, center.y + h * 0.3f)
        }
    }
    drawPath(path, color = MascotCyan, style = Stroke(width = 4.2f, cap = StrokeCap.Round))
}

private fun DrawScope.drawAngryEye(center: Offset, w: Float, h: Float, isLeft: Boolean) {
    val path = Path().apply {
        if (isLeft) {
            moveTo(center.x - w * 0.5f, center.y - h * 0.35f)
            lineTo(center.x + w * 0.5f, center.y + h * 0.25f)
        } else {
            moveTo(center.x - w * 0.5f, center.y + h * 0.25f)
            lineTo(center.x + w * 0.5f, center.y - h * 0.35f)
        }
    }
    drawPath(path, color = MascotWarningRed.copy(alpha = 0.5f), style = Stroke(width = 7f, cap = StrokeCap.Round))
    drawPath(path, color = MascotCyan, style = Stroke(width = 4.5f, cap = StrokeCap.Round))
}

private fun DrawScope.drawSereneEye(center: Offset, w: Float) {
    drawLine(
        color = MascotCyan,
        start = Offset(center.x - w * 0.48f, center.y),
        end = Offset(center.x + w * 0.48f, center.y),
        strokeWidth = 3.5f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawSleepingEyeArc(center: Offset, w: Float, h: Float) {
    val path = Path().apply {
        moveTo(center.x - w * 0.5f, center.y - h * 0.1f)
        quadraticTo(center.x, center.y + h * 0.4f, center.x + w * 0.5f, center.y - h * 0.1f)
    }
    drawPath(path, color = MascotCyan.copy(alpha = 0.7f), style = Stroke(width = 3.5f, cap = StrokeCap.Round))
}

private fun DrawScope.drawStarEye(center: Offset, radius: Float) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius * 1.3f)
        quadraticTo(center.x, center.y, center.x + radius * 1.3f, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + radius * 1.3f)
        quadraticTo(center.x, center.y, center.x - radius * 1.3f, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - radius * 1.3f)
        close()
    }
    drawCircle(color = MascotCyan.copy(alpha = 0.4f), radius = radius * 1.5f, center = center)
    drawPath(path, color = MascotCyan, style = Fill)
    drawCircle(color = Color.White, radius = radius * 0.3f, center = center)
}

// =========================================================================
// 5E. VISOR SPEECH WAVE (SPEAKING)
// =========================================================================
private fun DrawScope.drawVisorSpeechWaves(cx: Float, cy: Float, width: Float, wavePhase: Float) {
    val barCount = 5
    val barSpacing = width / (barCount + 1)

    for (i in 1..barCount) {
        val barX = cx - (width * 0.5f) + (i * barSpacing)
        val heightMultiplier = sin(wavePhase + (i * 1.2f))
        val barHeight = 8f + kotlin.math.abs(heightMultiplier) * 14f

        drawLine(
            color = MascotCyan,
            start = Offset(barX, cy - barHeight * 0.5f),
            end = Offset(barX, cy + barHeight * 0.5f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
    }
}

// =========================================================================
// 6. HOLOGRAPHIC OVERLAYS & STATE BADGES
// =========================================================================
private fun DrawScope.drawMascotStateOverlays(
    w: Float,
    h: Float,
    breathOffset: Float,
    tiltDegrees: Float,
    state: MascotState,
    expression: MascotExpression,
    breathProgress: Float,
    actionPulse: Float
) {
    val headCenterX = w * 0.5f
    val headCenterY = h * 0.36f + breathOffset

    when {
        // Curious Question Mark '?' above right ear
        expression == MascotExpression.CURIOSO -> {
            val markX = headCenterX + w * 0.32f
            val markY = headCenterY - h * 0.22f
            drawHologramSymbol("?", markX, markY, MascotCyan)
        }

        // Surprised Exclamation Mark '!' above head
        expression == MascotExpression.SORPRENDIDO -> {
            val markX = headCenterX + w * 0.26f
            val markY = headCenterY - h * 0.25f
            drawHologramSymbol("!", markX, markY, MascotCyan)
        }

        // Sleeping 'Z z z' drifting softly upwards
        expression == MascotExpression.DORMIDO || state == MascotState.SLEEPING -> {
            val z1Y = headCenterY - h * 0.2f - (sin(breathProgress) * 6f)
            val z2Y = headCenterY - h * 0.28f - (cos(breathProgress) * 6f)
            drawHologramText("z", headCenterX + w * 0.28f, z1Y, MascotCyan.copy(alpha = 0.7f), 12f)
            drawHologramText("Z", headCenterX + w * 0.36f, z2Y, MascotCyan.copy(alpha = 0.9f), 18f)
        }

        // Angry spark/vein mark
        expression == MascotExpression.ENOJADO -> {
            drawAngrySpark(headCenterX + w * 0.22f, headCenterY - h * 0.22f)
        }

        // Executing Action Holographic Tablet
        state == MascotState.EXECUTING_ACTION -> {
            drawActionHolographicTablet(w * 0.5f, h * 0.68f + breathOffset, w * 0.38f, h * 0.2f, actionPulse)
        }

        // Success Confirmation Badge (Checkmark ✓)
        state == MascotState.SUCCESS -> {
            drawSuccessBadge(w * 0.5f, h * 0.68f + breathOffset, w * 0.16f)
        }

        // Error Warning Badge (Triangle ⚠️)
        state == MascotState.ERROR -> {
            drawErrorBadge(headCenterX + w * 0.28f, headCenterY - h * 0.22f, w * 0.12f)
        }
    }
}

private fun DrawScope.drawActionHolographicTablet(cx: Float, cy: Float, width: Float, height: Float, pulse: Float) {
    // Glowing cyan transparent tablet screen
    val tabletRect = Rect(cx - width * 0.5f, cy - height * 0.5f, cx + width * 0.5f, cy + height * 0.5f)

    drawRoundRect(
        color = Color(0x3300E5FF),
        topLeft = Offset(tabletRect.left, tabletRect.top),
        size = Size(width, height),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawRoundRect(
        color = MascotCyan.copy(alpha = 0.85f),
        topLeft = Offset(tabletRect.left, tabletRect.top),
        size = Size(width, height),
        cornerRadius = CornerRadius(8f, 8f),
        style = Stroke(width = 2f)
    )

    // Holographic grid / progress lines inside tablet
    val lineCount = 3
    for (i in 0 until lineCount) {
        val y = tabletRect.top + (i + 1) * (height / (lineCount + 1))
        drawLine(
            color = MascotCyan.copy(alpha = 0.6f + pulse * 0.3f),
            start = Offset(tabletRect.left + width * 0.15f, y),
            end = Offset(tabletRect.right - width * 0.15f, y),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawSuccessBadge(cx: Float, cy: Float, radius: Float) {
    drawCircle(color = MascotSuccessGreen.copy(alpha = 0.35f), radius = radius * 1.4f, center = Offset(cx, cy))
    drawCircle(color = MascotBlack, radius = radius, center = Offset(cx, cy))
    drawCircle(color = MascotSuccessGreen, radius = radius, center = Offset(cx, cy), style = Stroke(width = 3f))

    // Checkmark ✓
    val checkPath = Path().apply {
        moveTo(cx - radius * 0.45f, cy)
        lineTo(cx - radius * 0.1f, cy + radius * 0.4f)
        lineTo(cx + radius * 0.48f, cy - radius * 0.35f)
    }
    drawPath(checkPath, color = MascotSuccessGreen, style = Stroke(width = 3.5f, cap = StrokeCap.Round))
}

private fun DrawScope.drawErrorBadge(cx: Float, cy: Float, radius: Float) {
    val triPath = Path().apply {
        moveTo(cx, cy - radius)
        lineTo(cx + radius * 0.86f, cy + radius * 0.6f)
        lineTo(cx - radius * 0.86f, cy + radius * 0.6f)
        close()
    }
    drawPath(triPath, color = MascotWarningRed.copy(alpha = 0.35f), style = Fill)
    drawPath(triPath, color = MascotWarningRed, style = Stroke(width = 2.5f))
    // Exclamation inside
    drawLine(color = Color.White, start = Offset(cx, cy - radius * 0.4f), end = Offset(cx, cy + radius * 0.1f), strokeWidth = 2.5f, cap = StrokeCap.Round)
    drawCircle(color = Color.White, radius = 1.8f, center = Offset(cx, cy + radius * 0.35f))
}

private fun DrawScope.drawAngrySpark(cx: Float, cy: Float) {
    val sparkPath = Path().apply {
        moveTo(cx - 6f, cy - 6f); lineTo(cx + 6f, cy + 6f)
        moveTo(cx + 6f, cy - 6f); lineTo(cx - 6f, cy + 6f)
    }
    drawPath(sparkPath, color = MascotWarningRed, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
}

private fun DrawScope.drawHologramSymbol(symbol: String, cx: Float, cy: Float, color: Color) {
    drawCircle(color = color.copy(alpha = 0.2f), radius = 14f, center = Offset(cx, cy))
    drawHologramText(symbol, cx - 4f, cy + 5f, color, 14f)
}

private fun DrawScope.drawHologramText(text: String, x: Float, y: Float, color: Color, sizeSp: Float) {
    // Drawn via circle markers or path approximations
    drawCircle(color = color, radius = sizeSp * 0.45f, center = Offset(x, y), style = Stroke(width = 1.8f))
}
