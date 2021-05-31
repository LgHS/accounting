package be.lghs.accounting.services;

import be.lghs.accounting.repositories.UserAccountNumberRepository;
import be.lghs.accounting.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAccountNumberService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final UserAccountNumberRepository userAccountNumberRepository;

    @Value("${lghs.accounting.deploy-url}")
    private final String deployUrl;

    public void addAccountNumber(UUID userId, String iban) {
        userAccountNumberRepository.addAccountNumber(userId, iban);
        notifyAdminOfPendingIbanValidations();
    }

    @Async
    public void notifyAdminOfPendingIbanValidations() {
        var admins = userRepository.findAdminEmails();
        log.info("sending pending iban validation mail to {} people", admins.size());

        for (String admin : admins) {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);
            try {
                helper.setFrom("accounting@liegehacker.space");
                helper.setReplyTo("admin@lghs.be");
                helper.setTo(admin);
                helper.setSubject("accounting: pending IBAN validation");
                helper.setText("Hi,\n\n" +
                    "A user has added a new IBAN to their account, please visit " + deployUrl + "/users/account-numbers to validate it.\n\n" +
                    "Have a nice day,\n" +
                    "-- Your accounting application.");
            } catch (MessagingException e) {
                log.error("couldn't send pending IBAN validation email to {}", admin, e);
            }
            mailSender.send(message);
        }
    }
}
