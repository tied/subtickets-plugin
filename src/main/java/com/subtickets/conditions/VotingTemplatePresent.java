package com.subtickets.conditions;

import com.subtickets.Constants;

import java.util.regex.Pattern;

public class VotingTemplatePresent extends AttachmentsPresent {

    @Override
    Pattern getAttachmentNamePattern() {
        return Pattern.compile(Constants.VOTING_TEMPLATE_FILE_NAME.replace(".", "\\."));
    }
}
