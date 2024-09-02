package com.shikanoko.study

interface NokoDestination {
    val route: String
}

object MainScreen : NokoDestination {
    override val route = "MainScreen"
}

object TestingScreen : NokoDestination {
    override val route = "TestingScreen"
}

object DBScreen : NokoDestination {
    override val route = "DBScreen"
}

