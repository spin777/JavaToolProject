package com.st.cloud.gametool.config;

import javafx.scene.control.ChoiceBox;
import lombok.Data;

/**
 * @author dev03
 */
@Data
public class ConfigVo {
    private String serverUrl;
    private String account;
    private String password;
    private Integer gameId;
    private Integer runNum;
    private Integer secondNum;
    private Integer carryNum;
    private Integer anteNum;
    private Integer betNum;
    private boolean isPay;
    private String choiceBox;
}
