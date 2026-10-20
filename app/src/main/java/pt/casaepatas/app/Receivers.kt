package pt.casaepatas.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Toca quando um alarme dispara: mostra a notificação e agenda a próxima ocorrência. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val key = intent.getStringExtra("key") ?: return
        val st = AppStore.read(context)
        Notifications.fire(context, st, key)
        Notifications.scheduleAll(context, st)
    }
}

/** Volta a agendar tudo depois de reiniciar o telemóvel, atualizar a app ou mudar a hora. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Notifications.ensureChannel(context)
        Notifications.scheduleAll(context, AppStore.read(context))
    }
}
