package edu.amrita.amritacafe.email

import android.annotation.SuppressLint
import io.reactivex.Completable
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object Mailer {
    @SuppressLint("CheckResult")
    fun sendMail(email: String, subject: String, message: String): Completable {

        return Completable.create { emitter ->

            //configure SMTP server
            val props: Properties = Properties().also {
                it.put("mail.smtp.host", "smtp.gmail.com")
                it.put("mail.smtp.socketFactory.port", "465")
                it.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                it.put("mail.smtp.auth", "true")
                it.put("mail.smtp.port", "465")
            }

            //Creating a session
            val session = Session.getDefaultInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(MyGmailAccountDetails.EMAIL, MyGmailAccountDetails.PASSWORD)
                }
            })

            try {
                MimeMessage(session).let { mime ->
                    mime.setFrom(InternetAddress(MyGmailAccountDetails.EMAIL))
                    //Adding receiver
                    mime.addRecipient(Message.RecipientType.TO, InternetAddress(email))
                    //Adding subject
                    mime.setSubject(subject)
                    //Adding message
                    mime.setText(message)
                    //send mail
                    Transport.send(mime)
                }

            } catch (e: MessagingException) {
                emitter.onError(e)
            }

            //ending subscription
            emitter.onComplete()
        }

    }

}