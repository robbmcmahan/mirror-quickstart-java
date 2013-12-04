/*
 * Copyright (C) 2013 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.glassware;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.mirror.Mirror;
import com.google.api.services.mirror.model.Command;
import com.google.api.services.mirror.model.Contact;
import com.google.api.services.mirror.model.Location;
import com.google.api.services.mirror.model.MenuItem;
import com.google.api.services.mirror.model.NotificationConfig;
import com.google.api.services.mirror.model.Subscription;
import com.google.api.services.mirror.model.TimelineItem;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

/**
 * Utility functions used when users first authenticate with this service
 *
 * @author Jenny Murphy - http://google.com/+JennyMurphy
 */
public class NewUserBootstrapper {
  private static final Logger LOG = Logger.getLogger(NewUserBootstrapper.class.getSimpleName());

  /**
   * Bootstrap a new user. Do all of the typical actions for a new user:
   * <ul>
   * <li>Creating a timeline subscription</li>
   * <li>Inserting a contact</li>
   * <li>Sending the user a welcome message</li>
   * </ul>
   */
  public static void bootstrapNewUser(HttpServletRequest req, String userId) throws IOException {
    Credential credential = AuthUtil.newAuthorizationCodeFlow().loadCredential(userId);

    // Create contact
    Contact starterProjectContact = new Contact();
    starterProjectContact.setId(MainServlet.CONTACT_ID);
    starterProjectContact.setDisplayName(MainServlet.CONTACT_NAME);
    starterProjectContact.setImageUrls(Lists.newArrayList(WebUtil.buildUrl(req,
        "/static/images/chipotle-tube-640x360.jpg")));
    starterProjectContact.setAcceptCommands(Lists.newArrayList(
        new Command().setType("TAKE_A_NOTE")));
    Contact insertedContact = MirrorClient.insertContact(credential, starterProjectContact);
    LOG.info("Bootstrapper inserted contact " + insertedContact.getId() + " for user " + userId);

    try {
      // Subscribe to timeline updates
      Subscription subscription =
          MirrorClient.insertSubscription(credential, WebUtil.buildUrl(req, "/notify"), userId,
              "timeline");
      LOG.info("Bootstrapper inserted subscription " + subscription
          .getId() + " for user " + userId);
    } catch (GoogleJsonResponseException e) {
      LOG.warning("Failed to create timeline subscription. Might be running on "
          + "localhost. Details:" + e.getDetails().toPrettyString());
    }

    // Send welcome timeline item
    
    
    Mirror mirrorClient = MirrorClient.getMirror(credential);
    Location location = mirrorClient.locations().get("latest").execute();
    
    LOG.info("New location is " + location.getLatitude() + ", " + location.getLongitude());
    
    MenuItem navAction = new MenuItem().setAction("NAVIGATE");
    MenuItem delAction = new MenuItem().setAction("DELETE");
    String mapImage = "<img src='glass://map?w=640&h=360&marker=0;" + location.getLatitude() + "," + location.getLongitude() + "' height='100%' width='100%'/>";
        
    MirrorClient.insertTimelineItem(
        credential,
        new TimelineItem()
            .setHtml(makeHtmlForCard(mapImage))
            .setNotification(new NotificationConfig().setLevel("DEFAULT")).setLocation(location)
            .setMenuItems(Lists.newArrayList(navAction,delAction)));
    
//    TimelineItem timelineItem = new TimelineItem();
//    timelineItem.setText("Remembered Location");
//    
//    
//    timelineItem.setNotification(new NotificationConfig().setLevel("DEFAULT"));
//    TimelineItem insertedItem = MirrorClient.insertTimelineItem(credential, timelineItem);
//    LOG.info("Bootstrapper inserted welcome message " + insertedItem.getId() + " for user "
//        + userId);
  }
  
  private static String makeHtmlForCard(String content) {
	    return "<article class='photo'>" + content + 
	    		"<div class='photo-overlay'/><section><div class='text-auto-size'><p class='yellow'>Remembered Location</p></div></section></article>";
	  }

}
