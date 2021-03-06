/*
Copyright (C) 2021 CYS4 Srl
See the file 'LICENSE' for copying permission
*/
package cys4.scanner;

import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IResponseInfo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import cys4.controller.Utils;
import cys4.model.ExtensionEntity;
import cys4.model.LogEntity;
import cys4.model.RegexEntity;
import cys4.ui.MainUI;

import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BurpLeaksScanner {

    private MainUI mainUI;
    private IExtensionHelpers helpers;
    private IBurpExtenderCallbacks callbacks;
    private List<LogEntity> logEntries = new ArrayList<>();
    private List<RegexEntity> regexList = new ArrayList<>();
    private List<ExtensionEntity> extensionList = new ArrayList<>();
    private ArrayList<String> l_blacklistedMimeTypes;
    private Gson _gson;

    public BurpLeaksScanner(MainUI mainUI, IBurpExtenderCallbacks callbacks, List<LogEntity> logEntries, List<RegexEntity> regexList, List<ExtensionEntity> extensionList) {
        // init params
        this.mainUI = mainUI;
        this.callbacks = callbacks;
        this.helpers = callbacks.getHelpers();
        this.logEntries = logEntries;
        this.regexList = regexList;
        this.extensionList = extensionList;
    }

    //
    //  method for analyzing the elements in Burp > Proxy > HTTP history
    //
    public void analyzeProxyHistory() {

        IHttpRequestResponse[] httpRequests;
        httpRequests = callbacks.getProxyHistory();

        LogEntity.setIdRequest(0); // responseId will start at 0

        for (IHttpRequestResponse httpProxyItem : httpRequests) {
            analyzeSingleMessage(httpProxyItem);
        }
    }

    //
    //  the main method that scan for regex in the single request body
    //
    private void analyzeSingleMessage(IHttpRequestResponse httpProxyItem) {

        // the condition check if the inScope variable is true or false; in the first case it checks if the httpProxyItem respects the "in scope" condition
        if ((httpProxyItem.getResponse() != null) && (!mainUI.isInScope() || callbacks.isInScope(helpers.analyzeRequest(httpProxyItem).getUrl()))) {
            IResponseInfo httpProxyItemResponse = helpers.analyzeResponse(httpProxyItem.getResponse());

            String mimeType = httpProxyItemResponse.getStatedMimeType().toUpperCase();
            // try to get the mime type from body instead of header
            if (mimeType.equals("")) {
                mimeType = httpProxyItemResponse.getInferredMimeType().toUpperCase();
            }

            if (isValidMimeType(mimeType)) {


                // convert from bytes to string the body of the request
                String responseBody = helpers.bytesToString(httpProxyItem.getResponse());
                for (RegexEntity entry : regexList) {

                        // if the box related to the regex in the Options tab of the extension is checked
                        if (entry!=null && entry.isActive()) {

                            String regex = entry.getRegex();
                            Pattern regex_pattern = Pattern.compile(regex);
                            Matcher regex_matcher = regex_pattern.matcher(responseBody);

                            while (regex_matcher.find()) {
                                // create a new log entry with the message details
                                addLogEntry(httpProxyItem, entry.getDescription() + " - " + entry.getRegex(), regex_matcher.group());
                            }

                        }
                    }


                for (ExtensionEntity entry : extensionList) {
                    if (entry.isActive()) {
                        URL requestURL = helpers.analyzeRequest(httpProxyItem).getUrl();
                        String extension = entry.getExtension();
                        if (extension.charAt(extension.length() - 1) != '$') {
                            extension = extension + '$';
                        }

                        Pattern extension_pattern = Pattern.compile(extension);
                        Matcher extension_matcher = extension_pattern.matcher(requestURL.toString());
                        // add the new entry if do not exist
                        if (extension_matcher.find()) {
                            addLogEntry(httpProxyItem, "EXT " + entry.getDescription() + " - " + extension, extension);
                        }
                    }
                }
            }
        }
    }

    private void addLogEntry(IHttpRequestResponse httpProxyItem, String description, String match) {
        // create a new log entry with the message details
        int row = logEntries.size();
        // the group method is used for retrieving the context in which the regex has matched
        LogEntity logEntry = new LogEntity(callbacks.saveBuffersToTempFiles(httpProxyItem), helpers.analyzeRequest(httpProxyItem).getUrl(), description, match);
        if (!logEntries.contains(logEntry)) {
            logEntries.add(logEntry);
            mainUI.logTableEntriesUIAddNewRow(row);
        }
    }

    private boolean isValidMimeType(String currentMimeType) {

        if(null == l_blacklistedMimeTypes)
        {
            l_blacklistedMimeTypes = new ArrayList<>();
            if(null == _gson) _gson= new Gson();

            Type tArrayListString = new TypeToken<ArrayList<String>>() {}.getType();
            List<String> lDeserializedJson = _gson.fromJson(Utils.readResourceFile("mime_types.json"), tArrayListString);
            for (String element:lDeserializedJson)
                l_blacklistedMimeTypes.add(element);
        }


        return !l_blacklistedMimeTypes.contains(currentMimeType);
    }
}
