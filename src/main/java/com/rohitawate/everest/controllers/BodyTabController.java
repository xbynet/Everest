/*
 * Copyright 2018 Rohit Awate.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rohitawate.everest.controllers;

import com.rohitawate.everest.controllers.codearea.EverestCodeArea;
import com.rohitawate.everest.controllers.codearea.EverestCodeArea.HighlightMode;
import com.rohitawate.everest.misc.Services;
import com.rohitawate.everest.misc.ThemeManager;
import com.rohitawate.everest.models.DashboardState;
import com.rohitawate.everest.models.requests.DataDispatchRequest;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.fxmisc.flowless.VirtualizedScrollPane;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.ResourceBundle;

/*
    Raw and Binary tabs are embedded in
    this FXML itself.
    URL encoded and Form tabs have special FXMLs.
 */
public class BodyTabController implements Initializable {
    @FXML
    private TabPane bodyTabPane;
    @FXML
    ComboBox<String> rawInputTypeBox;
    @FXML
    Tab rawTab, binaryTab, formTab, urlTab;
    @FXML
    TextField filePathField;
    @FXML
    private VBox rawVBox;

    EverestCodeArea rawInputArea;
    FormDataTabController formDataTabController;
    URLTabController urlTabController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        rawInputTypeBox.getItems().addAll("PLAIN TEXT", "JSON", "XML", "HTML");
        rawInputTypeBox.getSelectionModel().select(0);

        rawInputArea = new EverestCodeArea();
        ThemeManager.setSyntaxTheme(rawInputArea);
        rawInputArea.setPrefHeight(1500);   // Hack to make the EverestCodeArea stretch with the Composer
        rawVBox.getChildren().add(new VirtualizedScrollPane<>(rawInputArea));

        rawInputTypeBox.valueProperty().addListener(change -> {
            String type = rawInputTypeBox.getValue();
            HighlightMode mode;
            switch (type) {
                case "JSON":
                    mode = HighlightMode.JSON;
                    break;
                case "XML":
                    mode = HighlightMode.XML;
                    break;
                case "HTML":
                    mode = HighlightMode.HTML;
                    break;
                default:
                    mode = HighlightMode.PLAIN;
            }
            rawInputArea.setMode(mode);
        });

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/homewindow/FormDataTab.fxml"));
            formTab.setContent(loader.load());
            formDataTabController = loader.getController();

            loader = new FXMLLoader(getClass().getResource("/fxml/homewindow/URLTab.fxml"));
            Parent formTabContent = loader.load();
            ThemeManager.setTheme(formTabContent);
            urlTab.setContent(formTabContent);
            urlTabController = loader.getController();
        } catch (IOException e) {
            Services.loggingService.logSevere("Could not load URL tab.", e, LocalDateTime.now());
        }
    }

    /**
     * Returns a EverestRequest object initialized with the request body.
     */
    public DataDispatchRequest getBasicRequest(String requestType) {
        DataDispatchRequest request = new DataDispatchRequest(requestType);

        // Raw and binary types get saved in Body.
        // Form and URL encoded types use tuple objects
        if (rawTab.isSelected()) {
            String contentType;
            switch (rawInputTypeBox.getValue()) {
                case "PLAIN TEXT":
                    contentType = MediaType.TEXT_PLAIN;
                    break;
                case "JSON":
                    contentType = MediaType.APPLICATION_JSON;
                    break;
                case "XML":
                    contentType = MediaType.APPLICATION_XML;
                    break;
                case "HTML":
                    contentType = MediaType.TEXT_HTML;
                    break;
                default:
                    contentType = MediaType.TEXT_PLAIN;
            }
            request.setContentType(contentType);
            request.setBody(rawInputArea.getText());
        } else if (formTab.isSelected()) {
            request.setStringTuples(formDataTabController.getStringTuples());
            request.setFileTuples(formDataTabController.getFileTuples());
            request.setContentType(MediaType.MULTIPART_FORM_DATA);
        } else if (binaryTab.isSelected()) {
            request.setBody(filePathField.getText());
            request.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        } else if (urlTab.isSelected()) {
            request.setStringTuples(urlTabController.getStringTuples());
            request.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        }
        return request;
    }

    @FXML
    private void browseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose a binary file to add to request...");
        Window dashboardWindow = filePathField.getScene().getWindow();
        String filePath;
        try {
            filePath = fileChooser.showOpenDialog(dashboardWindow).getAbsolutePath();
        } catch (NullPointerException NPE) {
            filePath = "";
        }
        filePathField.setText(filePath);
    }

    public void setState(DashboardState dashboardState) {
        try {
            switch (dashboardState.getContentType()) {
                case MediaType.TEXT_PLAIN:
                    setRawTab(dashboardState, "PLAIN TEXT");
                    break;
                case MediaType.APPLICATION_JSON:
                    setRawTab(dashboardState, "JSON");
                    break;
                case MediaType.APPLICATION_XML:
                    setRawTab(dashboardState, "XML");
                    break;
                case MediaType.TEXT_HTML:
                    setRawTab(dashboardState, "HTML");
                    break;
                case MediaType.MULTIPART_FORM_DATA:
                    // For file tuples
                    for (Map.Entry entry : dashboardState.getFileTuples().entrySet())
                        formDataTabController.addFileField(entry.getKey().toString(), entry.getValue().toString());

                    // For string tuples
                    for (Map.Entry entry : dashboardState.getStringTuples().entrySet())
                        formDataTabController.addStringField(entry.getKey().toString(), entry.getValue().toString());
                    bodyTabPane.getSelectionModel().select(formTab);
                    break;
                case MediaType.APPLICATION_OCTET_STREAM:
                    filePathField.setText(dashboardState.getBody());
                    bodyTabPane.getSelectionModel().select(binaryTab);
                    break;
                case MediaType.APPLICATION_FORM_URLENCODED:
                    for (Map.Entry entry : dashboardState.getStringTuples().entrySet())
                        urlTabController.addField(entry.getKey().toString(), entry.getValue().toString());
                    bodyTabPane.getSelectionModel().select(urlTab);
                    break;
            }
        } catch (NullPointerException NPE) {
            Services.loggingService.logInfo("Dashboard loaded with blank request body.", LocalDateTime.now());
        }
    }

    private void setRawTab(DashboardState dashboardState, String contentType) {
        HighlightMode mode;

        switch (contentType) {
            case MediaType.APPLICATION_JSON:
                mode = HighlightMode.JSON;
                break;
            case MediaType.APPLICATION_XML:
                mode = HighlightMode.XML;
                break;
            case MediaType.TEXT_HTML:
                mode = HighlightMode.HTML;
                break;
            default:
                mode = HighlightMode.PLAIN;
        }

        rawInputArea.setText(dashboardState.getBody(), mode);
        rawInputTypeBox.getSelectionModel().select(contentType);
        bodyTabPane.getSelectionModel().select(rawTab);
    }
}
