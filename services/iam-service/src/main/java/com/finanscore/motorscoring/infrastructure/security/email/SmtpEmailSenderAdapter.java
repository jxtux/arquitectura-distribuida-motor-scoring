package com.finanscore.motorscoring.infrastructure.security.email;

import com.finanscore.motorscoring.application.security.port.out.EmailSenderPort;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public final class SmtpEmailSenderAdapter implements EmailSenderPort {
    private final JavaMailSender mailSender;
    private final String from;

    public SmtpEmailSenderAdapter(JavaMailSender mailSender, String from) {
        this.mailSender = mailSender; this.from = from;
    }

    @Override
    public void sendEmailVerificationCode(String email, String displayName, String code) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(email);
        msg.setSubject("Verifica tu cuenta FinanScore");
        msg.setText("Hola " + displayName + ",\n\nTu código de verificación es: " + code
            + "\n\nExpira en 10 minutos.");
        mailSender.send(msg);
    }
}
