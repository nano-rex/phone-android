package org.convoy.phone.models

sealed class Events {
    data object RefreshCallLog : Events()
}
