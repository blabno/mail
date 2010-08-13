package org.jboss.seam.mail.core;

import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.activation.URLDataSource;
import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import org.jboss.seam.mail.core.enumurations.ContentDisposition;
import org.jboss.seam.mail.core.enumurations.MailHeader;
import org.jboss.seam.mail.core.enumurations.MessagePriority;
import org.jboss.seam.mail.core.enumurations.RecipientType;
import org.jboss.weld.extensions.resourceLoader.ResourceProvider;

public abstract class BaseMailMessage
{
   private RootMimeMessage rootMimeMessage;
   private String charset;
   private Map<String, Attachment> attachments = new HashMap<String, Attachment>();
   private MimeMultipart rootMultipart = new MimeMultipart("mixed");
   private MimeMultipart relatedMultipart = new MimeMultipart("related");

   @Inject
   private ResourceProvider resourceProvider;

   @Inject
   public BaseMailMessage(Session session)
   {
      rootMimeMessage = new RootMimeMessage(session);
      charset = "UTF-8";
      setSentDate(new Date());
      setMessageID("<" + UUID.randomUUID().toString() + "@" + MailUtility.getHostName() + ">");
      initialize();
   }

   private void initialize()
   {
      try
      {
         rootMimeMessage.setContent(rootMultipart);
      }
      catch (MessagingException e)
      {
         throw new RuntimeException("Unable to set RootMultiPart", e);
      }
   }

   public void addRecipient(RecipientType recipientType, EmailContact emailContact)
   {
      try
      {
         rootMimeMessage.addRecipient(recipientType.getRecipientType(), MailUtility.getInternetAddress(emailContact));
      }
      catch (MessagingException e)
      {
         throw new RuntimeException("Unable to add recipient " + recipientType + ": " + emailContact.toString() + " to MIME message", e);
      }
   }

   public void addRecipients(RecipientType recipientType, EmailContact[] emailContacts)
   {
      try
      {
         rootMimeMessage.addRecipients(recipientType.getRecipientType(), MailUtility.getInternetAddressses(emailContacts));
      }
      catch (MessagingException e)
      {
         throw new RuntimeException("Unable to add " + recipientType + ":  Collection<Recipients>to MIME message", e);
      }
   }

   public void addRecipients(RecipientType recipientType, Collection<EmailContact> emailContacts)
   {
      try
      {
         rootMimeMessage.addRecipients(recipientType.getRecipientType(), MailUtility.getInternetAddressses(emailContacts));
      }
      catch (MessagingException e)
      {
      }
   }

   public void setFrom(String name, String address)
   {
      setFrom(new EmailContact(name, address));
   }

   public BaseMailMessage setFrom(EmailContact emailContact)
   {
      try
      {
         rootMimeMessage.setFrom(MailUtility.getInternetAddress(emailContact));
      }
      catch (MessagingException e)
      {
         throw new RuntimeException("Unable to add From Address:" + emailContact.getEmailAddress() + " to MIME message with charset: " + emailContact.getCharset(), e);
      }
      return this;
   }

   public void addTo(String name, String address)
   {
      addRecipient(RecipientType.TO, new EmailContact(name, address));
   }

   public void addCc(String name, String address)
   {
      addRecipient(RecipientType.CC, new EmailContact(name, address));
   }

   public void addBcc(String name, String address)
   {
      addRecipient(RecipientType.BCC, new EmailContact(name, address));
   }

   public void setSubject(String value)
   {
      setSubject(value, "UTF-8");
   }

   public void setSubject(String value, String charset)
   {
      try
      {
         rootMimeMessage.setSubject(value, charset);
      }
      catch (MessagingException e)
      {
         throw new RuntimeException("Unable to add subject:" + value + " to MIME message with charset: " + charset, e);
      }
   }

   public void setSentDate(Date date)
   {
      try
      {
         rootMimeMessage.setSentDate(date);
      }
      catch (MessagingException e)
      {
         throw new RuntimeException("Unable to set Sent Date on MimeMessage", e);
      }
   }

   public void setMessageID(String messageId)
   {
      rootMimeMessage.setMessageId(messageId);
   }

   public void setDeliveryReciept(String address)
   {
      setHeader(MailHeader.DELIVERY_RECIEPT.headerValue(), "<" + address + ">");
   }

   public void setReadReciept(String address)
   {
      setHeader(MailHeader.READ_RECIEPT.headerValue(), "<" + address + ">");
   }

   public void setImportance(MessagePriority messagePriority)
   {
      setHeader("X-Priority", messagePriority.getX_priority());
      setHeader("Priority", messagePriority.getPriority());
      setHeader("Importance", messagePriority.getImportance());
   }

   public void setHeader(String name, String value)
   {
      try
      {
         rootMimeMessage.setHeader(name, MimeUtility.encodeText(value));
      }
      catch (MessagingException e)
      {
         throw new RuntimeException("Unable to SET Header: + " + name + " to Value: " + value, e);
      }
      catch (UnsupportedEncodingException e)
      {
         throw new RuntimeException("Unable to SET Header: + " + name + " to Value: " + value, e);
      }
   }

   public void addHeader(String name, String value)
   {
      try
      {
         rootMimeMessage.addHeader(name, MimeUtility.encodeText(value));
      }
      catch (MessagingException e)
      {
         throw new RuntimeException("Unable to ADD Header: + " + name + " to Value: " + value, e);
      }
      catch (UnsupportedEncodingException e)
      {
         throw new RuntimeException("Unable to ADD Header: + " + name + " to Value: " + value, e);
      }
   }

   public void setText(String text)
   {
      try
      {
         rootMultipart.addBodyPart(buildTextBodyPart(text));
      }
      catch (MessagingException e)
      {
         throw new RuntimeException("Unable to add TextBody to MimeMessage", e);
      }
   }

   public void setHTML(String html)
   {
      MimeBodyPart relatedBodyPart = new MimeBodyPart();
      try
      {
         relatedMultipart.addBodyPart(buildHTMLBodyPart(html));
         relatedBodyPart.setContent(relatedMultipart);
         rootMultipart.addBodyPart(relatedBodyPart);
      }
      catch (MessagingException e)
      {
         throw new RuntimeException("Unable to add TextBody to MimeMessage", e);
      }
   }

   public void setHTMLTextAlt(String html, String text)
   {
      MimeBodyPart mixedBodyPart = new MimeBodyPart();

      MimeBodyPart relatedBodyPart = new MimeBodyPart();

      MimeMultipart alternativeMultiPart = new MimeMultipart("alternative");

      try
      {
         // Text must be the first or some HTML capable clients will fail to
         // render HTML bodyPart.
         alternativeMultiPart.addBodyPart(buildTextBodyPart(text));
         alternativeMultiPart.addBodyPart(buildHTMLBodyPart(html));

         relatedBodyPart.setContent(alternativeMultiPart);

         relatedMultipart.addBodyPart(relatedBodyPart);

         mixedBodyPart.setContent(relatedMultipart);

         rootMultipart.addBodyPart(mixedBodyPart);
      }
      catch (MessagingException e)
      {
         throw new RuntimeException("Unable to build HTML+Text Email", e);
      }
   }

   private MimeBodyPart buildTextBodyPart(String text)
   {
      MimeBodyPart textBodyPart = new MimeBodyPart();

      try
      {
         textBodyPart.setDisposition(ContentDisposition.INLINE.headerValue());
         textBodyPart.setText(text, charset);
      }
      catch (MessagingException e)
      {
         throw new RuntimeException("Unable to build TextBodyPart", e);
      }

      return textBodyPart;
   }

   private MimeBodyPart buildHTMLBodyPart(String html)
   {
      MimeBodyPart htmlBodyPart = new MimeBodyPart();

      try
      {
         htmlBodyPart.setDisposition(ContentDisposition.INLINE.headerValue());
         htmlBodyPart.setText(html, charset, "html");
      }
      catch (MessagingException e)
      {
         throw new RuntimeException("Unable to build HTMLBodyPart", e);
      }

      return htmlBodyPart;
   }

   public void addAttachmentImpl(File file, ContentDisposition contentDisposition)
   {
      Attachment attachment = new Attachment(file, file.getName(), contentDisposition);
      addAttachment(attachment);
   }

   public void addAttachmentImpl(File file, String fileName, ContentDisposition contentDisposition)
   {
      Attachment attachment = new Attachment(file, fileName, contentDisposition);
      addAttachment(attachment);
   }

   public void addAttachmentImpl(byte[] bytes, String fileName, String mimeType, ContentDisposition contentDisposition)
   {
      Attachment attachment = new Attachment(bytes, fileName, mimeType, contentDisposition);
      addAttachment(attachment);
   }

   public void addAttachmentImpl(byte[] bytes, String fileName, ContentDisposition contentDisposition)
   {
      Attachment attachment = new Attachment(bytes, fileName, "application/octetStream", contentDisposition);
      addAttachment(attachment);
   }

   public void addAttachmentImpl(String fileName, String mimeType, ContentDisposition contentDisposition)
   {
      InputStream inputStream = resourceProvider.loadResourceStream(fileName);

      if (inputStream == null)
      {
         throw new RuntimeException("InputStream was NULL for fileName: " + fileName);
      }

      Attachment attachment = new Attachment(inputStream, fileName, mimeType, contentDisposition);
      addAttachment(attachment);
   }

   public void addAttachmentImpl(String fileName, ContentDisposition contentDisposition)
   {
      addAttachmentImpl(fileName, "application/octetStream", contentDisposition);
   }

   public void addAttachmentImpl(URL url, String fileName, ContentDisposition contentDisposition)
   {
      Attachment attachment = new Attachment(new URLDataSource(url), fileName, contentDisposition);
      addAttachment(attachment);
   }

   private void addAttachment(Attachment attachment)
   {
      attachments.put(attachment.getAttachmentFileName(), attachment);
   }

   public Map<String, Attachment> getAttachments()
   {
      return attachments;
   }

   public MimeMessage getRootMimeMessage()
   {
      return rootMimeMessage;
   }

   public void finalizeMessage()
   {
      addAttachmentsToMessage();
   }

   public MimeMessage getFinalizedMessage()
   {
      finalizeMessage();
      return getRootMimeMessage();
   }

   public void send()
   {
      try
      {
         finalizeMessage();
         Transport.send(rootMimeMessage);
      }
      catch (MessagingException e)
      {
         throw new RuntimeException("Message Send Failed!", e);
      }
   }

   private void addAttachmentsToMessage()
   {
      for (Attachment a : attachments.values())
      {
         if (a.getContentDisposition() == ContentDisposition.ATTACHMENT)
         {
            try
            {
               rootMultipart.addBodyPart(a);
            }
            catch (MessagingException e)
            {
               throw new RuntimeException("Unable to Add STANDARD Attachment: " + a.getAttachmentFileName(), e);
            }
         }
         else if (a.getContentDisposition() == ContentDisposition.INLINE)
         {
            try
            {
               relatedMultipart.addBodyPart(a);
            }
            catch (MessagingException e)
            {
               throw new RuntimeException("Unable to Add INLINE Attachment: " + a.getAttachmentFileName(), e);
            }
         }
         else
         {
            throw new RuntimeException("Unsupported Attachment Content Disposition");
         }
      }
   }
}
