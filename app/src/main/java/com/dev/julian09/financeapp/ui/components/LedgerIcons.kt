package com.dev.julian09.financeapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.dev.julian09.financeapp.domain.model.AccountType

/** Icon keys stored in accounts.icon and categories.icon (max 30 chars on the server) and their outline icons. */
val ACCOUNT_ICONS: List<Pair<String, ImageVector>> =
    listOf(
        "wallet" to Icons.Outlined.AccountBalanceWallet,
        "bank" to Icons.Outlined.AccountBalance,
        "cash" to Icons.Outlined.Payments,
        "savings" to Icons.Outlined.Savings,
        "trending_up" to Icons.AutoMirrored.Outlined.TrendingUp,
        "money" to Icons.Outlined.AttachMoney,
    )

val CATEGORY_ICONS: List<Pair<String, ImageVector>> =
    listOf(
        "pets" to Icons.Outlined.Pets,
        "favorite" to Icons.Outlined.FavoriteBorder,
        "home" to Icons.Outlined.Home,
        "cart" to Icons.Outlined.ShoppingCart,
        "book" to Icons.AutoMirrored.Outlined.MenuBook,
        "gift" to Icons.Outlined.CardGiftcard,
    )

private val OTHER_ICONS =
    mapOf(
        "restaurant" to Icons.Outlined.Restaurant,
        "car" to Icons.Outlined.DirectionsCar,
        "movie" to Icons.Outlined.Movie,
        "health" to Icons.Outlined.LocalHospital,
        "school" to Icons.Outlined.School,
        "percent" to Icons.Outlined.Percent,
        "more" to Icons.Outlined.MoreHoriz,
        "work" to Icons.Outlined.Work,
    )

private val ALL_ICONS = ACCOUNT_ICONS.toMap() + CATEGORY_ICONS.toMap() + OTHER_ICONS

fun accountIcon(
    key: String?,
    type: AccountType,
): ImageVector =
    ALL_ICONS[key] ?: when (type) {
        AccountType.CASH -> Icons.Outlined.Payments
        AccountType.SAVINGS -> Icons.Outlined.Savings
        AccountType.CREDIT_CARD, AccountType.LOAN -> Icons.Outlined.CreditCard
    }

fun categoryIcon(key: String?): ImageVector = ALL_ICONS[key] ?: Icons.Outlined.Category

val TransferIcon: ImageVector = Icons.Outlined.SwapHoriz

fun accountTypeLabel(type: AccountType) =
    when (type) {
        AccountType.CASH -> "Efectivo"
        AccountType.SAVINGS -> "Cuenta de ahorros"
        AccountType.CREDIT_CARD -> "Tarjeta de crédito"
        AccountType.LOAN -> "Préstamo"
    }

/** For tight places such as the Home carousel. */
fun accountTypeShortLabel(type: AccountType) =
    when (type) {
        AccountType.CASH -> "Efectivo"
        AccountType.SAVINGS -> "Ahorros"
        AccountType.CREDIT_CARD -> "Tarjeta"
        AccountType.LOAN -> "Préstamo"
    }

/** Color tags for accounts (Nueva cuenta), stored as #RRGGBB in uppercase as the server expects. */
val ACCOUNT_COLORS = listOf("#4A3328", "#78716C", "#B03A2E", "#2F7A45", "#9A6B1F", "#1C1917")
