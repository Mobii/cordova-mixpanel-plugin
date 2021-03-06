package mobi.semparar.cordova.mixpanel;

import android.content.Context;
import android.text.TextUtils;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import java.util.Map;;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.LOG;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class JSONHelper {
    public static Map<String, Integer> jsonToMap(JSONObject json) throws JSONException {
        Map<String, Integer> retMap = new HashMap<String, Integer>();

        if(json != JSONObject.NULL) {
            retMap = toMap(json);
        }
        return retMap;
    }

    public static Map<String, Integer> toMap(JSONObject object) throws JSONException {
        Map<String, Integer> map = new HashMap<String, Integer>();

        Iterator<String> keysItr = object.keys();
        while(keysItr.hasNext()) {
            String key = keysItr.next();
            Integer value = object.optInt(key);

            map.put(key, value);
        }
        return map;
    }

    public static List<Object> toList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<Object>();
        for(int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if(value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }

            else if(value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }
}

public class MixpanelPlugin extends CordovaPlugin {

    private static String LOG_TAG = "MIXPANEL PLUGIN";
    private static MixpanelAPI mixpanel;

    private enum Action {


        // MIXPANEL API


        ALIAS("alias"),
        FLUSH("flush"),
        IDENTIFY("identify"),
        INIT("init"),
        RESET("reset"),
        TRACK("track"),
        GET_DISTINCT_ID("get_distinct_id"),

        // PEOPLE API


        PEOPLE_SET("people_set"),
        PEOPLE_IDENTIFY("people_identify"),
        PEOPLE_INCREMENT("people_increment"),
        PEOPLE_TRACK_CHARGE("people_track_charge"),

        SET_PUSH_REGISTRATION_ID("set_push_registration_id"),
        INITIALIZE_HANDLE_PUSH("initialize_handle_push");

        private final String name;
        private static final Map<String, Action> lookup = new HashMap<String, Action>();

        static {
            for (Action a : Action.values()) lookup.put(a.getName(), a);
        }

        private Action(String name) { this.name = name; }
        public String getName() { return name; }
        public static Action get(String name) { return lookup.get(name); }
    }


    /**
     * helper fn that logs the err and then calls the err callback
     */
    private void error(CallbackContext cbCtx, String message) {
        LOG.e(LOG_TAG, message);
        cbCtx.error(message);
    }


    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext cbCtx) {
        // throws JSONException
        Action act = Action.get(action);

        if (act == null){
            this.error(cbCtx, "unknown action");
            return false;
        }

        if (mixpanel == null && Action.INIT != act) {
            this.error(cbCtx, "you must initialize mixpanel first using \"init\" action");
            return false;
        }

        switch (act) {
            case ALIAS:
                return handleAlias(args, cbCtx);
            case FLUSH:
                return handleFlush(args, cbCtx);
            case IDENTIFY:
                return handleIdentify(args, cbCtx);
            case INIT:
                return handleInit(args, cbCtx);
            case RESET:
                return handleReset(args, cbCtx);
            case TRACK:
                return handleTrack(args, cbCtx);
            case PEOPLE_SET:
                return handlePeopleSet(args, cbCtx);
            case PEOPLE_IDENTIFY:
                return handlePeopleIdentify(args, cbCtx);
            case PEOPLE_INCREMENT:
                return handlePeopleIncrement(args, cbCtx);
            case PEOPLE_TRACK_CHARGE:
                return handlePeopleTrackCharge(args, cbCtx);
            case INITIALIZE_HANDLE_PUSH:
                return handleInitializePushHandling(args, cbCtx);
            case SET_PUSH_REGISTRATION_ID:
                return handleSetPushRegistrationId(args, cbCtx);
            case GET_DISTINCT_ID:
                return handleGetDistinctId(args, cbCtx);
            default:
                this.error(cbCtx, "unknown action");
                return false;
        }
    }


    @Override
    public void onDestroy() {
        if (mixpanel != null) {
            mixpanel.flush();
        }
        super.onDestroy();
    }


    //************************************************
    //  ACTION HANDLERS
    //   - return true:
    //     - to indicate action was executed with correct arguments
    //     - also if the action from sdk has failed.
    //  - return false:
    //     - arguments were wrong
    //************************************************

    private boolean handleAlias(JSONArray args, final CallbackContext cbCtx) {
        String aliasId = args.optString(0, "");
        String originalId = args.optString(1, null);
        if (TextUtils.isEmpty(aliasId)) {
            this.error(cbCtx, "missing alias id");
            return false;
        }
        mixpanel.alias(aliasId, originalId);
        cbCtx.success();
        return true;
    }


    private boolean handleFlush(JSONArray args, final CallbackContext cbCtx) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mixpanel.flush();
                cbCtx.success();
            }
        };
        cordova.getThreadPool().execute(runnable);
        cbCtx.success();
        return true;
    }


    private boolean handleIdentify(JSONArray args, final CallbackContext cbCtx) {
        String uniqueId = args.optString(0, mixpanel.getDistinctId().toString());
        mixpanel.identify(uniqueId);
        cbCtx.success(uniqueId);
        return true;
    }


    private boolean handleInit(JSONArray args, final CallbackContext cbCtx) {
        String token = args.optString(0, "");
        if (TextUtils.isEmpty(token)) {
            this.error(cbCtx, "missing token for mixpanel project");
            return false;
        }
        Context ctx = cordova.getActivity();
        mixpanel = MixpanelAPI.getInstance(ctx, token);
        cbCtx.success();
        return true;
    }


    private boolean handleReset(JSONArray args, final CallbackContext cbCtx) {
        mixpanel.reset();
        cbCtx.success();
        return true;
    }

    private boolean handleTrack(JSONArray args, final CallbackContext cbCtx) {
        String event = args.optString(0, "");
        if (TextUtils.isEmpty(event)) {
            this.error(cbCtx, "missing event name");
            return false;
        }

        JSONObject properties = args.optJSONObject(1);
        if (properties == null) {
            properties = new JSONObject();
        }
        mixpanel.track(event, properties);
        cbCtx.success();
        return true;
    }

    private boolean handleGetDistinctId(JSONArray args, final CallbackContext cbCtx) {
        cbCtx.success(mixpanel.getDistinctId().toString());
        return true;
    }

    private boolean handlePeopleIdentify(JSONArray args, final CallbackContext cbCtx) {
        String distinctId = args.optString(0, "");
        if (TextUtils.isEmpty(distinctId)) {
            this.error(cbCtx, "missing distinct id");
            return false;
        }
        mixpanel.getPeople().identify(distinctId);
        cbCtx.success();
        return true;
    }


    private boolean handlePeopleSet(JSONArray args, final CallbackContext cbCtx) {
        JSONObject properties = args.optJSONObject(0);
        if (properties == null) {
            this.error(cbCtx, "missing people properties object");
            return false;
        }
        mixpanel.getPeople().set(properties);
        cbCtx.success();
        return true;
    }

    private boolean handlePeopleIncrement(JSONArray args, final CallbackContext cbCtx) {
        if(args.optInt(0)==1){
            mixpanel.getPeople().increment(args.optString(1), args.optInt(2,1));
            cbCtx.success();
            return true;
        }else if(args.optInt(0)==2){
            try{
                mixpanel.getPeople().increment(JSONHelper.jsonToMap(args.optJSONObject(1)));
                cbCtx.success();
                return true;
            } catch(JSONException err){
                this.error(cbCtx, "JSON conversion error");
                return false;
            }
        }
        this.error(cbCtx, "missing people action type");
        return false;
    }

    private boolean handlePeopleTrackCharge(JSONArray args, final CallbackContext cbCtx) {
        cbCtx.success();
        return true;
    }

    private boolean handleInitializePushHandling(JSONArray args, final CallbackContext cbCtx){
        String projectId = args.optString(0, "");
        if (TextUtils.isEmpty(projectId)) {
            this.error(cbCtx, "missing projectId");
            return false;
        }
        mixpanel.getPeople().initPushHandling(projectId);
        cbCtx.success();
        return true;
    }

    private boolean handleSetPushRegistrationId(JSONArray args, final CallbackContext cbCtx){
        String registrationId = args.optString(0, "");
        if (TextUtils.isEmpty(registrationId)) {
            this.error(cbCtx, "missing registrationId");
            return false;
        }
        mixpanel.getPeople().setPushRegistrationId(registrationId);
        cbCtx.success();
        return true;
    }
}
