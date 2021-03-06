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
package com.silverpeas.form.displayers;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;
import org.apache.ecs.ElementContainer;
import org.apache.ecs.xhtml.a;
import org.apache.ecs.xhtml.div;
import org.apache.ecs.xhtml.img;
import org.apache.ecs.xhtml.input;

import org.silverpeas.attachment.AttachmentServiceFactory;
import org.silverpeas.attachment.model.SimpleDocument;
import org.silverpeas.attachment.model.SimpleDocumentPK;

import com.silverpeas.form.Field;
import com.silverpeas.form.FieldTemplate;
import com.silverpeas.form.FormException;
import com.silverpeas.form.PagesContext;
import com.silverpeas.form.Util;
import com.silverpeas.form.fieldType.FileField;
import com.silverpeas.util.EncodeHelper;
import com.silverpeas.util.FileUtil;
import com.silverpeas.util.StringUtil;
import com.silverpeas.util.web.servlet.FileUploadUtil;

import com.stratelia.silverpeas.silvertrace.SilverTrace;
import com.stratelia.webactiv.util.FileServerUtils;

/**
 * A displayer of a video. The underlying video player is FlowPlayer
 * (http://flowplayer.org/index.html).
 */
public class VideoFieldDisplayer extends AbstractFileFieldDisplayer {

  /**
   * The default width in pixels of the video display area.
   */
  public static final String DEFAULT_WIDTH = "425";
  /**
   * The default height in pixels of the video display area.
   */
  public static final String DEFAULT_HEIGHT = "300";
  /**
   * Should the video auto starts by default?
   */
  public static final boolean DEFAULT_AUTOPLAY = true;
  /**
   * The video display width parameter name.
   */
  public static final String PARAMETER_WIDTH = "width";
  /**
   * The video display height parameter name.
   */
  public static final String PARAMETER_HEIGHT = "height";
  /**
   * The video autostart parameter name.
   */
  public static final String PARAMETER_AUTOPLAY = "autoplay";
  public static final String CONTEXT_FORM_VIDEO = "XMLFormVideo";
  private static final String OPERATION_KEY = "Operation";
  private static final int DISPLAYED_HTML_OBJECTS = 2;

  /**
   * The different kinds of operation that can be applied into an attached video file.
   */
  private enum Operation {

    ADD, UPDATE, DELETION;
  }

  @Override
  public void displayScripts(final PrintWriter out, final FieldTemplate template,
      final PagesContext pageContext) throws IOException {
    checkFieldType(template.getTypeName(), "VideoFieldDisplayer.displayScripts");
    String language = pageContext.getLanguage();
    String fieldName = template.getFieldName();
    if (template.isMandatory() && pageContext.useMandatory()) {
      out.append("	if (isWhitespace(stripInitialWhitespace(field.value))) {\n")
          .append("		var ").append(fieldName).append("Value = document.getElementById('")
          .append(fieldName).append(Field.FILE_PARAM_NAME_SUFFIX).append("').value;\n")
          .append("   var ").append(fieldName).append("Operation = document.")
          .append(pageContext.getFormName()).append(".")
          .append(fieldName).append(OPERATION_KEY).append(".value;\n")
          .append("		if (").append(fieldName).append("Value=='' || ")
          .append(fieldName).append("Operation=='").append(Operation.DELETION.name()).append(
          "') {\n")
          .append("			errorMsg+=\"  - '")
          .append(EncodeHelper.javaStringToJsString(template.getLabel(language))).append("' ")
          .append(Util.getString("GML.MustBeFilled", language)).append("\\n \";\n")
          .append("			errorNb++;\n")
          .append("		}\n")
          .append("	}\n");
    }

    Util.includeFileNameLengthChecker(template, pageContext, out);
    Util.getJavascriptChecker(template.getFieldName(), pageContext, out);
  }

  @Override
  public void display(PrintWriter out, FileField field, FieldTemplate template,
      PagesContext pagesContext) throws FormException {
    checkFieldType(template.getTypeName(), "VideoFieldDisplayer.display");
    String attachmentId = field.getValue();
    if (!StringUtil.isDefined(attachmentId)) {
      attachmentId = "";
    }
    if (!template.isHidden()) {
      ElementContainer xhtmlcontainer = new ElementContainer();
      VideoPlayer videoPlayer = new VideoPlayer();
      videoPlayer.init(xhtmlcontainer);
      if (template.isReadOnly()) {
        displayVideo(videoPlayer, attachmentId, template, xhtmlcontainer, pagesContext);
      } else if (!template.isDisabled()) {
        displayVideoFormInput(videoPlayer, attachmentId, template, xhtmlcontainer, pagesContext);
      }

      out.println(xhtmlcontainer.toString());
    }
  }

  @Override
  public List<String> update(String attachmentId, FileField field, FieldTemplate template,
      PagesContext PagesContext) throws FormException {
    checkFieldType(field.getTypeName(), "VideoFieldDisplayer.update");
    List<String> attachmentIds = new ArrayList<String>();
    if (!StringUtil.isDefined(attachmentId)) {
      field.setNull();
    } else {
      field.setAttachmentId(attachmentId);
      attachmentIds.add(attachmentId);
    }
    return attachmentIds;
  }

  @Override
  public List<String> update(List<FileItem> items, FileField field, FieldTemplate template,
      PagesContext pageContext) throws FormException {
    List<String> attachmentIds = new ArrayList<String>();
    try {
      String fieldName = template.getFieldName();
      String attachmentId = uploadVideoFile(items, fieldName, pageContext);
      Operation operation = Operation.valueOf(FileUploadUtil.getParameter(items, fieldName
          + OPERATION_KEY));
      String currentAttachmentId = field.getAttachmentId();
      if ((isDeletion(operation, currentAttachmentId) || isUpdate(operation, attachmentId))
          && !pageContext.isCreation()) {
        deleteAttachment(currentAttachmentId, pageContext);
      }
      if (StringUtil.isDefined(attachmentId)) {
        attachmentIds.addAll(update(attachmentId, field, template, pageContext));
      }
    } catch (IOException ex) {
      SilverTrace.error("form", "VideoFieldDisplayer.update", "form.EXP_UNKNOWN_FIELD", null, ex);
    }

    return attachmentIds;
  }

  @Override
  public boolean isDisplayedMandatory() {
    return true;
  }

  @Override
  public int getNbHtmlObjectsDisplayed(FieldTemplate template, PagesContext pagesContext) {
    return DISPLAYED_HTML_OBJECTS;
  }

  /**
   * Checks the type of the field is as expected. The field must be of type file.
   *
   * @param typeName the name of the type.
   * @param contextCall the context of the call: which is the caller of this method. This parameter
   * is used for trace purpose.
   */
  private void checkFieldType(final String typeName, final String contextCall) {
    if (!Field.TYPE_FILE.equals(typeName)) {
      SilverTrace.info("form", contextCall, "form.INFO_NOT_CORRECT_TYPE", Field.TYPE_FILE);
    }
  }

  /**
   * Computes the URL of the video to display from the identifier of the attached video file and by
   * taking into account the displaying context.
   *
   * @param attachmentId the identifier of the attached video file.
   * @param pageContext the displaying page context.
   * @return the URL of the video file as String.
   */
  private String computeVideoURL(final String attachmentId, final PagesContext pageContext) {
    String videoURL = "";
    if (StringUtil.isDefined(attachmentId)) {
      if (attachmentId.startsWith("/")) {
        videoURL = attachmentId;
      } else {
        SimpleDocument attachment = AttachmentServiceFactory.getAttachmentService()
            .searchDocumentById(new SimpleDocumentPK(attachmentId, pageContext.getComponentId()),
            pageContext.getContentLanguage());
        if (attachment != null) {
          String webContext = FileServerUtils.getApplicationContext();
          videoURL = webContext + attachment.getAttachmentURL();
        }
      }
    }
    return videoURL;
  }

  /**
   * Displays the video refered by the specified URL into the specified XHTML container.
   *
   * @param videoPlayer the video player to display.
   * @param attachmentId the identifier of the attached file containing the video to display.
   * @param template the template of the field to which is mapped the video.
   * @param xhtmlContainer the XMLHTML container into which the video is displayed.
   */
  private void displayVideo(final VideoPlayer videoPlayer, final String attachmentId,
      final FieldTemplate template, final ElementContainer xhtmlContainer,
      final PagesContext pagesContext) {
    String videoURL = computeVideoURL(attachmentId, pagesContext);
    Map<String, String> parameters = template.getParameters(pagesContext.getLanguage());
    initVideoPlayer(videoPlayer, videoURL, parameters);
    videoPlayer.renderIn(xhtmlContainer);
  }

  /**
   * Displays the form part corresponding to the video input. The form input is a way to change or
   * to remove the video file if this one exists.
   *
   * @param videoPlayer the video player to display as a form input.
   * @param attachmentId the identifier of the attached file containing the video to display.
   * @param template the template of the field to which is mapped the video.
   * @param pagesContext the context of the displaying page.
   */
  private void displayVideoFormInput(final VideoPlayer videoPlayer,
      final String attachmentId, final FieldTemplate template,
      final ElementContainer xhtmlContainer, final PagesContext pagesContext) {
    String fieldName = template.getFieldName();
    String language = pagesContext.getLanguage();
    String deletionIcon = Util.getIcon("delete");
    String deletionLab = Util.getString("removeFile", language);
    String videoURL = computeVideoURL(attachmentId, pagesContext);
    Operation defaultOperation = Operation.ADD;

    if (!videoURL.isEmpty()) {
      defaultOperation = Operation.UPDATE;
      Map<String, String> parameters = template.getParameters(pagesContext.getLanguage());
      parameters.remove(PARAMETER_WIDTH);
      parameters.remove(PARAMETER_HEIGHT);
      initVideoPlayer(videoPlayer, videoURL, parameters);

      // a link to the deletion operation
      img deletionImage = new img();
      deletionImage.setAlt(deletionLab).setSrc(deletionIcon).setWidth(15).setHeight(15).setAlt(
          deletionLab).setTitle(deletionLab);
      a removeLink = new a();
      removeLink.setHref("#")
          .addElement(deletionImage)
          .setOnClick("javascript: document.getElementById('" + fieldName
          + "Video').style.display='none'; document." + pagesContext.getFormName() + "."
          + fieldName + OPERATION_KEY + ".value='" + Operation.DELETION.name() + "';");
      div videoDiv = new div();
      videoDiv.setID(fieldName + "Video");
      videoDiv.setClass("video");
      videoPlayer.renderIn(videoDiv);
      videoDiv.addElement("&nbsp;");
      videoDiv.addElement(removeLink);

      xhtmlContainer.addElement(videoDiv);
    }

    // the input from which a video file can be selected
    input fileInput = new input();
    fileInput.setID(fieldName);
    fileInput.setType("file");
    fileInput.setSize(50);
    fileInput.setName(fieldName);
    input attachmentInput = new input();
    attachmentInput.setType("hidden").setName(fieldName + Field.FILE_PARAM_NAME_SUFFIX).setValue(
        attachmentId).setID(fieldName + Field.FILE_PARAM_NAME_SUFFIX);
    input operationInput = new input();
    operationInput.setType("hidden").setName(fieldName + OPERATION_KEY).setValue(defaultOperation.
        name()).setID(fieldName + OPERATION_KEY);
    div selectionDiv = new div();
    selectionDiv.setID(fieldName + "Selection");
    selectionDiv.addElement(fileInput);
    selectionDiv.addElement(attachmentInput);
    selectionDiv.addElement(operationInput);
    if (template.isMandatory() && pagesContext.useMandatory()) {
      selectionDiv.addElement(Util.getMandatorySnippet());
    }
    xhtmlContainer.addElement(selectionDiv);
  }

  /**
   * Initializes the specified video player with the specified URL and the specified parameters.
   *
   * @param videoPlayer the video player to set up.
   * @param videoURL the URL of the video to play.
   * @param parameters the parameters from which the video player will be initialized (height,
   * width, ...)
   */
  private void initVideoPlayer(final VideoPlayer videoPlayer, String videoURL,
      Map<String, String> parameters) {
    String width = (parameters.containsKey(PARAMETER_WIDTH) ? parameters.get(PARAMETER_WIDTH)
        : DEFAULT_WIDTH) + "px";
    String height = (parameters.containsKey(PARAMETER_HEIGHT) ? parameters.get(PARAMETER_HEIGHT)
        : DEFAULT_HEIGHT) + "px";
    boolean autoplay = (parameters.containsKey(PARAMETER_AUTOPLAY)
        ? Boolean.valueOf(parameters.get(PARAMETER_AUTOPLAY)) : false);
    videoPlayer.setVideoURL(videoURL);
    videoPlayer.setAutoplay(autoplay);
    videoPlayer.setWidth(width);
    videoPlayer.setHeight(height);
  }

  /**
   * Uploads the file containing the video and that is identified by the specified field name.
   *
   * @param items the items of the form. One of them is containg the video file.
   * @param itemKey the key of the item containing the video.
   * @param pageContext the context of the page displaying the form.
   * @throws Exception if an error occurs while uploading the video file.
   * @return the identifier of the uploaded attached video file.
   */
  private String uploadVideoFile(final List<FileItem> items, final String itemKey,
      final PagesContext pageContext) throws IOException {
    String attachmentId = "";

    FileItem item = FileUploadUtil.getFile(items, itemKey);
    if (!item.isFormField()) {
      String componentId = pageContext.getComponentId();
      String userId = pageContext.getUserId();
      String objectId = pageContext.getObjectId();
      String logicalName = item.getName();
      if (StringUtil.isDefined(logicalName)) {
        logicalName = FileUtil.getFilename(logicalName);
        SimpleDocument document = createSimpleDocument(objectId, componentId, item, logicalName,
            userId);
        attachmentId = document.getId();
      }
    }
    return attachmentId;
  }

  /**
   * Is the specified operation is a deletion?
   *
   * @param operation the operation.
   * @param attachmentId the identifier of the attachment on which the operation is.
   * @return true if the operation is a deletion, false otherwise.
   */
  private boolean isDeletion(final Operation operation, final String attachmentId) {
    return StringUtil.isDefined(attachmentId) && operation == Operation.DELETION;
  }

  /**
   * Is the specified operation is an update?
   *
   * @param operation the operation.
   * @param attachmentId the identifier of the attachment on which the operation is.
   * @return true if the operation is an update, false otherwise.
   */
  private boolean isUpdate(final Operation operation, final String attachmentId) {
    return StringUtil.isDefined(attachmentId) && operation == Operation.UPDATE;
  }
}
