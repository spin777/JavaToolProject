package com.st.cloud.gametool.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.st.cloud.gametool.config.ConfigManager;
import com.st.cloud.gametool.config.ConfigVo;
import com.st.cloud.gametool.proto.ToolsProto;
import com.st.cloud.gametool.tools.GameVo;
import com.st.cloud.gametool.websocket.WebSocket;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.Getter;

import java.io.FileWriter;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author dev03
 */
@Getter
public class AppController {
    /**
     * 服务器地址
     */
    public TextField serverUrl;
    /**
     * 账号输入框
     */
    @FXML
    TextField account;
    /**
     * 密码输入框
     */
    @FXML
    TextField password;
    /**
     * 登录按钮
     */
    @FXML
    Button login;

    /**
     * 游戏ID输入框
     */
    @FXML
    TextField gameIdTf;
    /**
     * 链接游戏按钮
     */
    @FXML
    Button linkGame;

    private final AtomicBoolean linkState = new AtomicBoolean(false);


    /**
     * 运行次数
     */
    @FXML
    TextField runNum;
    /**
     * 携带金额
     */
    @FXML
    TextField carryNum;
    /**
     * 底注积分
     */
    @FXML
    TextField anteNum;
    /**
     * 单注
     */
    @FXML
    TextField betNum;
    /**
     * 运行游戏按钮
     */
    @FXML
    Button runGame;
    /**
     * 次数日志
     */
    @FXML
    public Label logLabel;
    /**
     * 日志区域
     */
    @FXML
    TextArea logTa;


    private String token;

    @FXML
    public void initialize() {
        //设置数值监听
        numberListener(gameIdTf);
        numberListener(runNum);
        numberListener(carryNum);
        numberListener(anteNum);
        numberListener(betNum);
        //设置缓存配置
        ConfigVo cvo = ConfigManager.loaderConfig();
        if (Objects.nonNull(cvo)) {
            setNumberValue(serverUrl, cvo.getServerUrl());
            setNumberValue(account, cvo.getAccount());
            setNumberValue(password, cvo.getPassword());
            setNumberValue(gameIdTf, cvo.getGameId());
            setNumberValue(runNum, cvo.getRunNum());
            setNumberValue(carryNum, cvo.getCarryNum());
            setNumberValue(anteNum, cvo.getAnteNum());
            setNumberValue(betNum, cvo.getBetNum());
        }
        //  登录按钮点击事件
        login.setOnAction(event -> login());
        //  链接游戏按钮点击事件
        linkGame.setOnAction(event -> linkGame());
        //  运行游戏按钮点击事件
        runGame.setOnAction(event -> runGame());
    }

    /**
     * 设置组件只能输入数值
     */
    void numberListener(TextField textField) {
        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d+")) {
                textField.setText(oldVal);
            }
        });
    }

    /**
     * 数值设置
     */
    void setNumberValue(TextField textField, Object value) {
        if (Objects.nonNull(value)) {
            textField.setText(value.toString());
        }
    }

    /**
     * 保存配置
     */
    public void saveConfig() {
        ConfigManager.saveConfig(this);
    }

    /**
     * 显示提示框
     */
    void showCloseAlert(String content) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("警告!");
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.getDialogPane().getStyleClass().add("information");
            alert.showAndWait();
        });
    }

    /**
     * 登录游戏
     */
    void login() {
        String account = this.account.getText();
        String password = this.password.getText();
        if (StrUtil.isBlank(account) || StrUtil.isBlank(password)) {
            showCloseAlert("请输入正确的账号密码");
            return;
        }
        String url = this.serverUrl.getText() + "/gateway/member/user/login";
        JSONObject json = new JSONObject();
        json.set("username", account);
        json.set("password", password);
        json.set("equipment", IdUtil.simpleUUID());
        json.set("loginVersion", "3.2.1");
        json.set("loginPlatform", "PC");
        json.set("loginChannel", "PC");
        try {
            String body;
            try {
                body = HttpRequest.post(url)
                        .header("Content-Type", "application/json")
                        .header("Origin", "localhost")
                        .body(json.toJSONString(0)).timeout(5000).execute().body();
            } catch (Exception e) {
                showCloseAlert("登录超时了! 检查网络");
                body = null;
            }
            if (Objects.nonNull(body)) {
                json = JSONUtil.parseObj(body);
                boolean success = json.getBool("success");
                if (success) {
                    String code = json.getStr("code");
                    if ("00000".equalsIgnoreCase(code)) {
                        token = json.getStr("data");
                        login.setText("登录成功");
                        login.setDisable(true);
                    } else {
                        String message = json.getStr("message");
                        showCloseAlert(message);
                    }
                } else {
                    showCloseAlert("登录失败! 检查账号密码");
                }
            }
        } catch (Exception e) {
            showCloseAlert("代码错误! 联系制作者");
        }
        saveConfig();
    }

    private WebSocket webSocket;

    public void close() {
        if (Objects.nonNull(webSocket)) {
            webSocket.close();
        }
        if (Objects.nonNull(thread)) {
            thread.interrupt();
        }

    }

    int gameId;

    /**
     * 链接游戏
     */
    void linkGame() {
        if (linkState.get()) {
            if (Objects.nonNull(webSocket)) {
                close();
            }
        } else {
            if (StrUtil.isBlank(token)) {
                showCloseAlert("请先登录!");
            } else {
                gameId = Integer.parseInt(gameIdTf.getText());
                if (gameId < 1) {
                    showCloseAlert("请输入正确的游戏ID");
                } else {
                    webSocket = new WebSocket(serverUrl.getText(), token, gameId, 0, this);
                    webSocket.connect();
                }
            }
            saveConfig();
        }
    }

    Thread thread;
    AtomicBoolean runState = new AtomicBoolean(false);
    GameVo gameVo = null;
    String path = "C:/Users/Admin/Desktop/";
    Map<Integer, String> logMap = new ConcurrentHashMap<>();

    /**
     * 运行游戏
     */
    void runGame() {
        if (!linkState.get()) {
            showCloseAlert("请先链接游戏!");
            return;
        }
        if (runState.get()) {
            endRunGame();
            return;
        }
        int runNum = Integer.parseInt(this.runNum.getText());
        if (runNum < 1) {
            showCloseAlert("请输入正确的运行次数");
            return;
        }
        logMap.clear();
        runState.set(true);
        runGame.setText("正在运行");
        gameVo = new GameVo();
        gameVo.setBet(Long.parseLong(betNum.getText()));
        gameVo.setScore(Long.parseLong(anteNum.getText()));
        gameVo.setCarry(new AtomicLong(Long.parseLong(carryNum.getText())));
        gameVo.setWin(new AtomicLong(0));
        gameVo.setAllBet(new AtomicLong(0));
        gameVo.setRunNum(runNum);
        path += gameId + ".txt";
        String writerStr = String.format("%s,开始携带额度:%s\n", DateUtil.now(), gameVo.getCarry().get());
        logMap.put(0, writerStr);
        javafx.application.Platform.runLater(() -> logTa.setText(writerStr));
        send();
    }

    public void endRunGame() {
        javafx.application.Platform.runLater(() -> {
            runState.set(false);
            runGame.setText("运行");
            gameVo = null;
            path = "C:/Users/Admin/Desktop/";
        });
    }


    void writerText(String writerStr) {
        try (FileWriter writer = new FileWriter(path)) {
            writer.write(writerStr);
            writer.flush();
        } catch (Exception e) {
            javafx.application.Platform.runLater(() -> logTa.appendText(e.getMessage()));
        }
    }

    short msgId = -8888;

    void send() {
        Thread.startVirtualThread(() -> {
            int runNum = gameVo.getRunNum();
            for (int i = 1; i <= runNum; i++) {
                ThreadUtil.sleep(1);
                int finalI = i;
                javafx.application.Platform.runLater(() -> logLabel.setText(String.format("第 %s 局", finalI)));
                ToolsProto.ClientReq.Builder builder = ToolsProto.ClientReq.newBuilder();
                builder.setScore(gameVo.getScore());
                builder.setBet(gameVo.getBet());
                webSocket.sendMessage(msgId, builder);
            }
        });
    }

    ToolsProto.ClientRes toClientRes(byte[] bytes) {
        ToolsProto.ClientRes res;
        try {
            res = ToolsProto.ClientRes.parseFrom(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return res;
    }


    public void onMessage(short code, byte[] bytes) {
        if (code != msgId) {
            return;
        }
        int runNum = gameVo.getCount().incrementAndGet();

        long carry = gameVo.getCarry().addAndGet(-gameVo.getBet());
        long allBet = gameVo.getAllBet().addAndGet(gameVo.getBet());
        long allWin = gameVo.getWin().get();

        ToolsProto.ClientRes res = toClientRes(bytes);
        long win = res.getWin();
        String rtp = res.getRtp();
        boolean isFree = res.getIsFree();
        if (win > 0) {
            allWin = gameVo.getWin().addAndGet(win);
            carry = gameVo.getCarry().addAndGet(win);
        }

        String writerStr = String.format("%s,第%s局,%s,变动后:%s,得分:%s,作弊值:%s\n", DateUtil.now(), runNum, isFree ? "免费" : "常规", carry, win, rtp);
        logMap.put(runNum, writerStr);
        if (runNum % 1000 == 0) {
            String finalWriterStr = writerStr;
            javafx.application.Platform.runLater(() -> logTa.appendText(finalWriterStr));
        }
        if (runNum >= gameVo.getRunNum()) {
            writerStr = String.format("%s,结束:当前总携带:%s,总压:%s,总得分:%s,返奖率:%s,总运行:%s 毫秒\n", DateUtil.now(), carry,
                    allBet, allWin, NumberUtil.div(allWin, allBet, 2), System.currentTimeMillis() - gameVo.getStartTime());
            logMap.put(runNum + 1, writerStr);

            String finalWriterStr1 = writerStr;
            javafx.application.Platform.runLater(() -> logTa.appendText(finalWriterStr1));

            StringBuilder sb = new StringBuilder();
            logMap.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getKey)).forEach(entry -> sb.append(entry.getValue()));

            writerText(sb.toString());

            endRunGame();
            showCloseAlert("运行任务结束");
        }

    }
}