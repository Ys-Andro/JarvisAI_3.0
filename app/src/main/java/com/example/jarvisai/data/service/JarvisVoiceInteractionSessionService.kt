package com.example.jarvisai.data.service

import android.app.assist.AssistContent
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import com.example.MainActivity

class JarvisVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return object : VoiceInteractionSession(this) {
            override fun onHandleAssist(data: Bundle?, structure: AssistStructure?, content: AssistContent?) {
                super.onHandleAssist(data, structure, content)
                launchJarvis()
            }

            override fun onShow(args: Bundle?, showFlags: Int) {
                super.onShow(args, showFlags)
                launchJarvis()
            }

            private fun launchJarvis() {
                try {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        action = Intent.ACTION_ASSIST
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    finish()
                } catch (_: Exception) {}
            }
        }
    }
}
