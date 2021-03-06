/**
 * Copyright (C) 2000 - 2012 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of the GPL, you may
 * redistribute this Program in connection with Free/Libre Open Source Software ("FLOSS")
 * applications as described in Silverpeas's FLOSS exception. You should have received a copy of the
 * text describing the FLOSS exception, and it is also available here:
 * "http://www.silverpeas.org/docs/core/legal/floss_exception.html"
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package com.stratelia.silverpeas.notificationserver.channel.smtp;

import com.silverpeas.util.EncodeHelper;
import com.silverpeas.util.StringUtil;
import com.silverpeas.util.i18n.I18NHelper;
import com.stratelia.silverpeas.notificationserver.NotificationData;
import com.stratelia.silverpeas.notificationserver.NotificationServerException;
import com.stratelia.silverpeas.notificationserver.channel.AbstractListener;
import com.stratelia.silverpeas.silvertrace.SilverTrace;
import com.stratelia.webactiv.beans.admin.AdminReference;
import com.stratelia.webactiv.util.ResourceLocator;
import com.stratelia.webactiv.util.exception.SilverpeasException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.MessageListener;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.commons.lang3.CharEncoding;

import static com.silverpeas.util.MailUtil.*;

import static com.stratelia.silverpeas.notificationserver.channel.smtp.SMTPConstant.SECURE_TRANSPORT;
import static com.stratelia.silverpeas.notificationserver.channel.smtp.SMTPConstant.SIMPLE_TRANSPORT;

@MessageDriven(activationConfig = {
  @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
  @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "AutoAcknowledge"),
  @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "CHANNEL='SMTP'"),
  @ActivationConfigProperty(propertyName = "destination", propertyValue =
      "java:/queue/notificationsQueue")},
    description = "Message driven bean to send notifications by email")
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class SMTPListener extends AbstractListener implements MessageListener {

  private static final long serialVersionUID = -241712070051475710L;

  public SMTPListener() {
  }

  /**
   * listener of NotificationServer JMS message
   *
   * @param msg the message recieved
   */
  @Override
  public void onMessage(javax.jms.Message msg) {
    try {
      SilverTrace.info("smtp", "SMTPListner.onMessage()", "root.MSG_GEN_PARAM_VALUE",
          "JMS Message = " + msg);
      processMessage(msg);
    } catch (NotificationServerException e) {
      SilverTrace.error("smtp", "SMTPListner.onMessage()", "smtp.EX_CANT_PROCESS_MSG",
          "JMS Message = " + msg + ", Payload = " + (payLoad == null ? "" : payLoad), e);
    }
  }

  @Override
  public void send(NotificationData notification) throws NotificationServerException {
    // process the target param string, containing the FROM and the SUBJECT email fields.
    Map<String, Object> keyValue = notification.getTargetParam();
    String tmpFromString = (String) keyValue.get("FROM");
    String tmpSubjectString = (String) keyValue.get("SUBJECT");
    String tmpUrlString = (String) keyValue.get("URL");
    String tmpLanguageString = (String) keyValue.get("LANGUAGE");
    String tmpAttachmentIdString = (String) keyValue.get("ATTACHMENTID");
    String tmpSourceString = (String) keyValue.get("SOURCE");

    if (StringUtil.isDefined(tmpSourceString)) {
      tmpSubjectString = tmpSourceString + " : " + tmpSubjectString;
    }

    if (tmpLanguageString == null) {
      tmpLanguageString = I18NHelper.defaultLanguage;
    }

    ResourceLocator messages = new ResourceLocator(
        "org.silverpeas.notificationserver.channel.smtp.multilang.smtpBundle", tmpLanguageString);
    if (tmpFromString == null) {
      throw new NotificationServerException("SMTPListner.send()", SilverpeasException.ERROR,
          "smtp.EX_MISSING_FROM");
    } else {
      String body = notification.getMessage();
      // Transform text to html format
      body = EncodeHelper.convertWhiteSpacesForHTMLDisplay(body + "\n\n");
      if (tmpUrlString != null) {
        body += "<a href=\"" + tmpUrlString + "\" target=_blank>" + messages.getString("clickHere")
            + "</a> " + messages.getString("ToAccessDocument");
      }
      if (tmpAttachmentIdString == null) {
        sendEmail(tmpFromString, notification.getSenderName(), notification.getTargetReceipt(),
            tmpSubjectString, body, true);
      } else {
        // For the moment, send the email without attachment
        sendEmail(tmpFromString, notification.getSenderName(), notification.getTargetReceipt(),
            tmpSubjectString, body, false);
      }
    }
  }

  /**
   * send email to destination using SMTP protocol and JavaMail 1.3 API (compliant with MIME
   * format).
   *
   * @param pFrom : from field that will appear in the email header.
   * @param personalName :
   * @see {@link InternetAddress}
   * @param pTo : the email target destination.
   * @param pSubject : the subject of the email.
   * @param pMessage : the message or payload of the email.
   */
  private void sendEmail(String pFrom, String personalName, String pTo, String pSubject,
      String pMessage, boolean htmlFormat) throws NotificationServerException {
    // retrieves system properties and set up Delivery Status Notification
    // @see RFC1891
    Properties properties = System.getProperties();
    properties.put("mail.smtp.host", getMailServer());
    properties.put("mail.smtp.auth", String.valueOf(isAuthenticated()));
    javax.mail.Session session = javax.mail.Session.getInstance(properties, null);
    session.setDebug(isDebug()); // print on the console all SMTP messages.
    Transport transport = null;
    try {
      InternetAddress fromAddress = getAuthorizedEmailAddress(pFrom, personalName);
      InternetAddress replyToAddress = null;
      InternetAddress[] toAddress = null;
      // parsing destination address for compliance with RFC822
      try {
        toAddress = InternetAddress.parse(pTo, false);
        if (!AdminReference.getAdminService().getAdministratorEmail().equals(pFrom)
            && (!fromAddress.getAddress().equals(pFrom) || isForceReplyToSenderField())) {
          replyToAddress = new InternetAddress(pFrom, false);
          if (StringUtil.isDefined(personalName)) {
            replyToAddress.setPersonal(personalName, CharEncoding.UTF_8);
          }
        }
      } catch (AddressException e) {
        SilverTrace.warn("smtp", "SMTPListner.sendEmail()", "root.MSG_GEN_PARAM_VALUE",
            "From = " + pFrom + ", To = " + pTo);
      }
      MimeMessage email = new MimeMessage(session);
      email.setFrom(fromAddress);
      if (replyToAddress != null) {
        email.setReplyTo(new InternetAddress[]{replyToAddress});
      }
      email.setRecipients(javax.mail.Message.RecipientType.TO, toAddress);
      email.setHeader("Precedence", "list");
      email.setHeader("List-ID", fromAddress.getAddress());
      String subject = pSubject;
      if (subject == null) {
        subject = "";
      }
      String content = pMessage;
      if (content == null) {
        content = "";
      }
      email.setSubject(subject, CharEncoding.UTF_8);
      if (content.toLowerCase().contains("<html>") || htmlFormat) {
        email.setContent(content, "text/html; charset=\"UTF-8\"");
      } else {
        email.setText(content, CharEncoding.UTF_8);
      }
      email.setSentDate(new Date());

      // create a Transport connection (TCP)
      if (isSecure()) {
        transport = session.getTransport(SECURE_TRANSPORT);
      } else {
        transport = session.getTransport(SIMPLE_TRANSPORT);
      }
      if (isAuthenticated()) {
        SilverTrace.info("smtp", "SMTPListner.sendEmail()", "root.MSG_GEN_PARAM_VALUE", "Host = "
            + getMailServer() + " Port=" + getPort() + " User=" + getLogin());
        transport.connect(getMailServer(), getPort(), getLogin(), getPassword());
      } else {
        transport.connect();
      }

      transport.sendMessage(email, toAddress);
    } catch (MessagingException e) {
      Logger.getLogger(getClass().getSimpleName()).log(Level.SEVERE, e.getMessage(), e);
    } catch (UnsupportedEncodingException e) {
      Logger.getLogger(getClass().getSimpleName()).log(Level.SEVERE, e.getMessage(), e);
    } catch (Exception e) {
      throw new NotificationServerException("SMTPListner.sendEmail()", SilverpeasException.ERROR,
          "smtp.EX_CANT_SEND_SMTP_MESSAGE", e);
    } finally {
      if (transport != null) {
        try {
          transport.close();
        } catch (Exception e) {
          SilverTrace.
              error("smtp", "SMTPListner.sendEmail()", "root.EX_IGNORED", "ClosingTransport", e);
        }
      }
    }
  }
}
