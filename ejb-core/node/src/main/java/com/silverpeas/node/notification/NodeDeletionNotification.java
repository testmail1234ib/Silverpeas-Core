/**
 * Copyright (C) 2000 - 2012 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception.  You should have received a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "http://www.silverpeas.org/docs/core/legal/floss_exception.html"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.silverpeas.node.notification;

import com.silverpeas.notification.NotificationSource;
import com.silverpeas.notification.SilverpeasNotification;
import com.silverpeas.notification.SilverpeasNotificationCause;
import com.stratelia.webactiv.util.node.model.NodePK;

/**
 * Notification about the deletion of a nodes in a given component instance.
 */
public class NodeDeletionNotification extends SilverpeasNotification {

  private static final long serialVersionUID = -5651929204480502420L;

  protected NodeDeletionNotification(final NodePK node) {
    super(new NotificationSource().withComponentInstanceId(node.getInstanceId()),
        SilverpeasNotificationCause.DELETION, node);
  }

  public NodePK getNodePK() {
    return (NodePK) getObject();
  }
}
