//
// $Id$

package com.threerings.everything.client.util;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;

import com.threerings.gwt.ui.Popups;
import com.threerings.gwt.ui.SmartFileUpload;
import com.threerings.gwt.util.Console;

/**
 * Provides a simple "Upload" button that uploads a piece of media to the server and provides the
 * file name to a listener.
 */
public class MediaUploader extends FormPanel
{
    public static interface Listener
    {
        /** Called to inform the listener that new media has been uploaded. */
        void mediaUploaded (String name);
    }

    public MediaUploader (Listener listener)
    {
        addStyleName("mediaUploader");
        _listener = listener;

        HorizontalPanel controls = new HorizontalPanel();
        setWidget(controls);
        controls.setStyleName("Controls");

        setAction("upload"); // relative path to the upload servlet
        setEncoding(FormPanel.ENCODING_MULTIPART);
        setMethod(FormPanel.METHOD_POST);

        _upload = new SmartFileUpload();
        _upload.setName("image");
        _upload.addValueChangeHandler(new ValueChangeHandler<String>() {
            public void onValueChange (ValueChangeEvent<String> event) {
                String toUpload = _upload.getFilename();
                if (toUpload.length() > 0 && !toUpload.equals(_submitted)) {
                    submit();
                }
            }
        });
        controls.add(_upload);

        addSubmitHandler(new SubmitHandler() {
            public void onSubmit (SubmitEvent event) {
                // don't let them submit until they plug in a file...
                if (_upload.getFilename().length() == 0) {
                    event.cancel();
                }
            }
        });
        addSubmitCompleteHandler(new SubmitCompleteHandler() {
            public void onSubmitComplete (SubmitCompleteEvent event) {
                String result = event.getResults();
                result = (result == null) ? "" : result.trim();
                if (result.length() > 0) {
                    Popups.errorNear(result, _upload);
                } else {
                    _submitted = _upload.getFilename();
                }
            }
        });
    }

    @Override // from Widget
    public void onLoad ()
    {
        super.onLoad();
        configureBridge();
        _active = this;
    }

    @Override // from Widget
    public void onUnload ()
    {
        super.onUnload();
        _active = null;
    }

    protected void uploadComplete (String name)
    {
        _listener.mediaUploaded(name);
    }

    protected void uploadError (String code)
    {
        Popups.errorNear("Upload error: " + Errors.xlate(code), _upload);
    }

    /**
     * This is called from our magical JavaScript method by JavaScript code received from the
     * server as a response to our file upload POST request.
     */
    protected static void reportUploadComplete (String name)
    {
        // for some reason the strings that come in from JavaScript are not "real" and if we just
        // pass them straight on through to GWT, freakoutery occurs (of the non-hand-waving
        // variety); so we convert them hackily to GWT strings here
        if (_active != null) {
            _active.uploadComplete(""+name);
        } else {
            Console.log("Dropping uploaded media", "name", name);
        }
    }

    /**
     * This is called from our magical JavaScript method by JavaScript code received from the
     * server to display an internal error message to the user.
     */
    protected static void reportUploadError (String code)
    {
        if (_active != null) {
            _active.uploadError(""+code);
        } else {
            Console.log("Dropping upload error", "code", code);
        }
    }

    /**
     * This wires up a sensibly named function that our POST response JavaScript code can call.
     */
    protected static native void configureBridge () /*-{
        $wnd.uploadComplete = function (name) {
           @com.threerings.everything.client.util.MediaUploader::reportUploadComplete(Ljava/lang/String;)(name);
        };
        $wnd.uploadError = function (code) {
           @com.threerings.everything.client.util.MediaUploader::reportUploadError(Ljava/lang/String;)(code);
        };
    }-*/;

    protected Listener _listener;
    protected SmartFileUpload _upload;
    protected String _submitted;

    protected static MediaUploader _active;
}
