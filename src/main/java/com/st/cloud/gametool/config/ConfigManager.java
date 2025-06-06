package com.st.cloud.gametool.config;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.st.cloud.gametool.controller.AppController;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * @author dev03
 */
public class ConfigManager {
    /**
     * 配置文件路径
     */
    private static final String PATH = "C:/SpinConfig.json";

    /**
     * 加载配置
     */
    public static ConfigVo loaderConfig() {
        try {
            String jsonStr = FileUtil.readString(new File(PATH), StandardCharsets.UTF_8);
            return JSONUtil.toBean(jsonStr, ConfigVo.class);
        } catch (Exception ignored) {
        }
        return null;
    }

    public static void saveConfig(AppController app) {
        try {
            ConfigVo cvo = new ConfigVo();
            cvo.setServerUrl(app.getServerUrl().getText());
            cvo.setAccount(app.getAccount().getText());
            cvo.setPassword(app.getPassword().getText());
            cvo.setGameId(Integer.parseInt(app.getGameIdTf().getText()));
            cvo.setRunNum(Integer.parseInt(app.getRunNum().getText()));
            cvo.setCarryNum(Integer.parseInt(app.getCarryNum().getText()));
            cvo.setAnteNum(Integer.parseInt(app.getAnteNum().getText()));
            cvo.setBetNum(Integer.parseInt(app.getBetNum().getText()));
            FileUtil.writeString(JSONUtil.toJsonStr(cvo), new File(PATH), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }


}
