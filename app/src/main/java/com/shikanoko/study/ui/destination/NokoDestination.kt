package com.shikanoko.study.ui.destination

interface NokoDestination {
    val route: String
}

object MainScreen : NokoDestination {
    override val route = "MainScreen"
}

object TestingScreen : NokoDestination {
    override val route = "TestingScreen"
}

object ReviewScreen : NokoDestination {
    override val route = "ReviewScreen"
}

object DBScreen : NokoDestination {
    override val route = "DBScreen"
}

object MinnaScreen : NokoDestination {
    override val route = "MinnaScreen"
}

object StatisticsScreen : NokoDestination {
    override val route = "StatisticsScreen"
}

object StatisticsDetailScreen : NokoDestination {
    override val route = "StatisticsDetailScreen"
}

