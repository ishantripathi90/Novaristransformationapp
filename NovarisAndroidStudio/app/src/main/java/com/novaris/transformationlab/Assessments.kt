package com.novaris.transformationlab

import androidx.compose.ui.graphics.Color

/** Original website product identifiers: never rename slugs independently of PHP. */
data class Assessment(
    val key: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val minutes: String,
    val group: String,
    val accent: Color,
    val anonymous: Boolean = false
)

val ASSESSMENTS = listOf(
    Assessment("entronovaris", "Entronovaris™", "Organisational vitality", "Understand where organisational energy is lost and where leadership attention will make the difference.", "12–15 min", "Organisation", Color(0xFF176B75)),
    Assessment("authorityflow", "AuthorityFlow™", "Authority & execution", "Uncover how decisions, accountability, autonomy and escalation turn authority into action.", "15–20 min", "Organisation", Color(0xFF7759B5)),
    Assessment("growth_readiness", "Growth-Readiness Matrix™", "Founder dependency & scale", "Understand growth potential, founder dependency and the organisational work required to scale.", "12–15 min", "Organisation", Color(0xFFD48F47)),
    Assessment("relatiq", "RELATIQ™", "Workplace relationships", "Explore interpersonal dynamics shaping engagement, direction and connection at work.", "12–15 min", "People", Color(0xFFBB648A)),
    Assessment("team_dynamics", "Team Dynamics Map", "Team work styles", "Map work currents and team-style patterns to understand how people work together.", "10–12 min", "Teams", Color(0xFF3F7692)),
    Assessment("team_latitude", "Team Latitude Map", "Psychological safety", "Discover how safely team members can speak up, ask for help and learn through experimentation.", "10–12 min", "Teams", Color(0xFF43856D), anonymous = true),
    Assessment("meridian", "MEridian™", "Personality profile", "Explore six personality dimensions through the original 56-statement assessment.", "8–10 min", "Personal", Color(0xFF6079BD)),
    Assessment("equilibrium", "EQuilibrium™", "Emotional intelligence", "Reflect on six learnable emotional-intelligence skills through the original profile.", "8–10 min", "Personal", Color(0xFFAE7049))
)

val DeepNavy = Color(0xFF10283C)
val Ink = Color(0xFF1B3449)
val Muted = Color(0xFF708391)
val Canvas = Color(0xFFF7F8F5)
val Teal = Color(0xFF087E78)
val Mint = Color(0xFFE7F5F1)
val Rule = Color(0xFFE7ECEB)
