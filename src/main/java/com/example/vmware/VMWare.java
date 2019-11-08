package com.example.vmware;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.util.i18n.PreferredLocales;

import java.util.List;

import static org.forgerock.openam.auth.node.api.Action.goTo;

@Node.Metadata(outcomeProvider = VMWare.MyOutcomeProvider.class, configClass = VMWare.Config.class)

public class VMWare implements Node {
    private final Config config;
    private final CoreWrapper coreWrapper;
    private final static String DEBUG_FILE = "vmWare";
    protected Debug debug = Debug.getInstance(DEBUG_FILE);
    JsonValue context_json;

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        debug.error("+++     starting vmware");

        context_json = context.sharedState.copy();
        String search_key = context_json.get("device_id").asString();

        UserInfo userinfo = new UserInfo(config.awServer(), config.awKey(), config.awAdmin(), config.awPassword());
        String status = userinfo.getStatus(config.awIsCompliant(), search_key); // qry could be for either "is" ENROLLED or COMPLIANT
        Action action = null ;

        if (status.equals("compliant")) {
            action = goTo(MyOutcome.COMPLIANT).build();
        } else if (status.equals("noncompliant")) {
            action = goTo(MyOutcome.NONCOMPLIANT).build();
        } else if (status.equals("unknown")) {
            action = goTo(MyOutcome.UNKNOWN).build();
        } else {
            action = goTo(MyOutcome.CONNECTION_ERROR).build();
        }
        return action;
    }


    public enum MyOutcome {
        /**
         * Successful parsing of cert for a dev id.
         */
        COMPLIANT,
        /**
         * dev id found in cert but device isn't compliant
         */
        NONCOMPLIANT,
        /**
         * no device found with ID from cert
         */
        UNKNOWN,
        /**
         * no connection to mdm
         */
        CONNECTION_ERROR,
    }

    private Action.ActionBuilder goTo(MyOutcome outcome) {
        return Action.goTo(outcome.name());
    }

    public static class MyOutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            return ImmutableList.of(
                    new Outcome(MyOutcome.COMPLIANT.name(), "Compliant"),
                    new Outcome(MyOutcome.NONCOMPLIANT.name(), "Non Compliant"),
                    new Outcome(MyOutcome.UNKNOWN.name(), "Unknown"),
                    new Outcome(MyOutcome.CONNECTION_ERROR.name(), "Connection Error"));
        }
    }

    public interface Config {

        @Attribute(order = 100)
        default String awServer() {
            return "https://as1506.awmdm.com";
        }

        @Attribute(order = 200)
        default String awKey() {
            return "0/Sdna8d8oLI9BZpPp6ZE1TGB4hkVKayc9cZVTznblw=";
        }

        @Attribute(order = 500)
        default String awIsCompliant() {
            return "API/mdm/devices/compliance?searchBy=Udid&id=";
        }

        @Attribute(order = 600)
        default String awAdmin() {
            return "email@javaservlets.net";
        }

        @Attribute(order = 700)
        default String awPassword() {
            return "Passw0rd";
        }

    }


    @Inject
    public VMWare(@Assisted Config config, CoreWrapper coreWrapper) throws NodeProcessException {
        this.config = config;
        this.coreWrapper = coreWrapper;
    }


}