package com.itman.datastream.engine.debezium;


import java.io.*;
import java.util.*;
import java.util.Date;

/**
 * 增强版Debezium偏移量解析器
 * 支持解析Kafka Connect存储的offset文件，并提供offset重置功能
 */
public class FileOffsetBackingStoreParser {

    public static void main(String[] args) throws Exception {
        // 测试代码 - 实际使用时请注释掉
        // 步骤一: 查找binlog事件起始位置
//         args = new String[] {
//                 "--find-start-pos",
//                 "mysql-bin.000002",
//                 "1646",
//                 "host=192.168.239.128",
//                 "port=3306",
//                 "user=root",
//                 "password=root"
//         };
        
        // 步骤二: 重置offset（使用找到的起始位置）
         args = new String[] {
                 "--reset",
                 "E:\\dev\\data-stream\\debeziumFile\\debeziumEngine-754\\offset.dat",
                 "file=mysql-bin.000002",
                 "pos=1273"  // 使用 --find-start-pos 找到的起始位置
         };

        // 命令行参数解析
        if (args.length > 0 && "--reset".equals(args[0])) {
            // 重置offset模式
            if (args.length < 3) {
                printUsage();
                return;
            }
            String filePath = args[1];
            Map<String, String> params = parseResetParams(args, 2);
            resetOffset(filePath, params);
        } else if (args.length > 0 && "--find-start-pos".equals(args[0])) {
            // 查找binlog事件起始位置模式
            if (args.length < 4) {
                System.out.println("用法: --find-start-pos <binlog文件名> <位置> [MySQL连接参数]");
                System.out.println("示例: --find-start-pos mysql-bin.000001 1688");
                System.out.println("示例: --find-start-pos mysql-bin.000001 1688 host=192.168.1.1 port=3306 user=root password=root");
                return;
            }
            String binlogFile = args[1];
            long position = Long.parseLong(args[2]);
            Map<String, String> connParams = parseResetParams(args, 3);
            findBinlogStartPosition(binlogFile, position, connParams);
        } else {
            // 默认解析模式
            String filePath = args.length > 0 ? args[0] : "E:\\dev\\superstar\\superstar-debezium\\file\\debezium\\qm\\offset.dat";

            //尝试Kafka Connect格式解析
            System.out.println("\n=== Kafka Connect格式解析 ===");
            parseKafkaConnectFormat(filePath);
        }
    }

    /**
     * 打印使用说明
     */
    private static void printUsage() {
        System.out.println("Debezium Offset解析器使用说明:");
        System.out.println("\n1. 解析offset文件:");
        System.out.println("   java com.superstar.debezium.tool.FileOffsetBackingStoreParser [filepath]");
        System.out.println("   示例: java com.superstar.debezium.tool.FileOffsetBackingStoreParser /path/to/cdc-offsets.dat");
        System.out.println("\n2. 查找binlog事件起始位置:");
        System.out.println("   java com.superstar.debezium.tool.FileOffsetBackingStoreParser --find-start-pos <binlog文件名> <位置> [连接参数]");
        System.out.println("   示例: java com.superstar.debezium.tool.FileOffsetBackingStoreParser --find-start-pos mysql-bin.000001 1688");
        System.out.println("   示例: java com.superstar.debezium.tool.FileOffsetBackingStoreParser --find-start-pos mysql-bin.000001 1688 host=192.168.1.1 port=3306 user=root password=root");
        System.out.println("   说明: 当Debezium记录的pos是事件结束位置时，使用此功能查找事件的起始位置");
        System.out.println("\n3. 重置offset到指定位置:");
        System.out.println("   java com.superstar.debezium.tool.FileOffsetBackingStoreParser --reset <filepath> <参数>");
        System.out.println("   参数格式: key1=value1 key2=value2 ...");
        System.out.println("   支持参数:");
        System.out.println("     - file=<binlog文件名>       例如: file=mysql-bin.000001 (必需)");
        System.out.println("     - pos=<位置>                例如: pos=1000 (必需)");
        System.out.println("     - gtids=<GTID集合>          例如: gtids=813f8807-98d0-11ee-8ecf-0242ac110004:1-100");
        System.out.println("     - ts_sec=<时间戳>           例如: ts_sec=1764230500 (新建文件时默认当前时间)");
        System.out.println("     - event=<事件编号>          例如: event=0 (默认0，修改pos/file时自动重置为0)");
        System.out.println("     - server_id=<服务器ID>     例如: server_id=1 (默认1)");
        System.out.println("     - row=<行号>                例如: row=0 (默认0)");
        System.out.println("     - connector=<连接器名>      例如: connector=mysql-debezium-connect (新建文件时使用)");
        System.out.println("     - server=<服务器名>        例如: server=mysql-debezium-connect (新建文件时使用)");
        System.out.println("   示例: java com.superstar.debezium.tool.FileOffsetBackingStoreParser --reset /path/to/cdc-offsets.dat file=mysql-bin.000001 pos=154");
        System.out.println("   说明: 如果offset文件不存在，会自动创建新文件；如果存在，会更新现有数据");
        System.out.println("\n重要提示:");
        System.out.println("  - 重置offset会修改原文件，请确保已备份!");
        System.out.println("  - pos 必须是有效的 binlog 事件边界位置，否则会导致 'bogus data' 错误");
        System.out.println("  - Debezium记录的pos通常是事件结束位置，需要使用 --find-start-pos 查找起始位置");
        System.out.println("  - 建议使用 SHOW BINLOG EVENTS 或 mysqlbinlog 工具查找正确的 binlog 位置");
        System.out.println("  - 修改 pos 或 file 时，event 字段会自动重置为 0（除非明确指定）");
    }

    /**
     * 解析重置参数
     */
    private static Map<String, String> parseResetParams(String[] args, int startIndex) {
        Map<String, String> params = new HashMap<>();
        for (int i = startIndex; i < args.length; i++) {
            String arg = args[i];
            int eqIndex = arg.indexOf("=");
            if (eqIndex > 0) {
                String key = arg.substring(0, eqIndex);
                String value = arg.substring(eqIndex + 1);
                params.put(key, value);
            }
        }
        return params;
    }

    /**
     * 重置offset到指定位置
     */
    private static void resetOffset(String filePath, Map<String, String> params) {
        System.out.println("\n========== 重置Offset ==========");
        System.out.println("文件路径: " + filePath);
        System.out.println("参数: " + params);

        File offsetFile = new File(filePath);
        Map<byte[], byte[]> offsetMap = null;
        File backupFile = null;

        try {
            // 检查文件是否存在
            if (!offsetFile.exists()) {
                System.out.println("\n提示: offset文件不存在，将创建新文件");
                
                // 确保目录存在
                File parentDir = offsetFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                    System.out.println("已创建目录: " + parentDir.getAbsolutePath());
                }
                
                // 创建新的offset Map
                offsetMap = new HashMap<>();
                
                // 生成Key（连接器标识）
                String connectorName = params.getOrDefault("connector", "mysql-debezium-connect");
                String serverName = params.getOrDefault("server", connectorName);
                String keyJson = String.format("{\"schema\":null,\"payload\":[\"%s\",{\"server\":\"%s\"}]}", 
                    connectorName, serverName);
                byte[] keyBytes = keyJson.getBytes("UTF-8");
                
                // 生成Value（offset信息）
                String valueJson = createOffsetValueJson(params);
                byte[] valueBytes = valueJson.getBytes("UTF-8");
                
                offsetMap.put(keyBytes, valueBytes);
                
                System.out.println("\n--- 新建的Offset ---");
                System.out.println("Key: " + keyJson);
                System.out.println("Value: " + valueJson);
                
                // 显示关键字段
                if (valueJson.contains("\"file\":")) {
                    System.out.println("  file: " + extractJsonValue(valueJson, "file"));
                }
                if (valueJson.contains("\"pos\":")) {
                    System.out.println("  pos: " + extractJsonValue(valueJson, "pos"));
                }
                if (valueJson.contains("\"event\":")) {
                    System.out.println("  event: " + extractJsonValue(valueJson, "event"));
                }
                
                // 验证JSON格式
                if (!isValidJson(valueJson)) {
                    System.out.println("  ⚠ 警告: JSON格式可能不正确!");
                } else {
                    System.out.println("  ✓ JSON格式验证通过");
                }
                
            } else {
                // 文件存在，使用现有逻辑
                System.out.println("\n提示: offset文件已存在，将更新现有数据");
                System.out.println("警告: 正在修改offset文件，请确保已备份!");
                
                backupFile = new File(filePath + ".backup." + System.currentTimeMillis());
                
                // 先备份原文件
                backupOffsetFile(filePath, backupFile);
                System.out.println("已创建备份文件: " + backupFile.getAbsolutePath());

                // 读取现有offset
                offsetMap = readOffsetFile(filePath);

                if (offsetMap.isEmpty()) {
                    System.out.println("错误: offset文件为空或无法读取");
                    return;
                }

                // 显示修改前的offset
                System.out.println("\n--- 修改前的Offset ---");
                for (Map.Entry<byte[], byte[]> entry : offsetMap.entrySet()) {
                    String keyJson = new String(entry.getKey(), "UTF-8");
                    String valueJson = new String(entry.getValue(), "UTF-8");
                    System.out.println("Key: " + keyJson);
                    System.out.println("Value: " + valueJson);
                    
                    // 提取关键字段用于对比
                    if (valueJson.contains("\"file\":")) {
                        System.out.println("  当前 file: " + extractJsonValue(valueJson, "file"));
                    }
                    if (valueJson.contains("\"pos\":")) {
                        System.out.println("  当前 pos: " + extractJsonValue(valueJson, "pos"));
                    }
                    if (valueJson.contains("\"event\":")) {
                        System.out.println("  当前 event: " + extractJsonValue(valueJson, "event"));
                    }
                    System.out.println();

                    // 修改value
                    String newValueJson = modifyOffsetJson(valueJson, params);
                    byte[] newValueBytes = newValueJson.getBytes("UTF-8");

                    // 更新map
                    offsetMap.put(entry.getKey(), newValueBytes);
                }
            }

            // 显示修改前的offset
            System.out.println("\n--- 修改前的Offset ---");
            for (Map.Entry<byte[], byte[]> entry : offsetMap.entrySet()) {
                String keyJson = new String(entry.getKey(), "UTF-8");
                String valueJson = new String(entry.getValue(), "UTF-8");
                System.out.println("Key: " + keyJson);
                System.out.println("Value: " + valueJson);
                
                // 提取关键字段用于对比
                if (valueJson.contains("\"file\":")) {
                    System.out.println("  当前 file: " + extractJsonValue(valueJson, "file"));
                }
                if (valueJson.contains("\"pos\":")) {
                    System.out.println("  当前 pos: " + extractJsonValue(valueJson, "pos"));
                }
                if (valueJson.contains("\"event\":")) {
                    System.out.println("  当前 event: " + extractJsonValue(valueJson, "event"));
                }
                System.out.println();

                // 修改value
                String newValueJson = modifyOffsetJson(valueJson, params);
                byte[] newValueBytes = newValueJson.getBytes("UTF-8");

                // 更新map
                offsetMap.put(entry.getKey(), newValueBytes);
            }

            // 如果是更新现有文件，显示修改后的offset
            if (offsetFile.exists()) {
                System.out.println("\n--- 修改后的Offset ---");
                for (Map.Entry<byte[], byte[]> entry : offsetMap.entrySet()) {
                    String keyJson = new String(entry.getKey(), "UTF-8");
                    String valueJson = new String(entry.getValue(), "UTF-8");
                    System.out.println("Key: " + keyJson);
                    System.out.println("Value: " + valueJson);
                    
                    // 提取关键字段用于验证
                    if (valueJson.contains("\"file\":")) {
                        System.out.println("  修改后 file: " + extractJsonValue(valueJson, "file"));
                    }
                    if (valueJson.contains("\"pos\":")) {
                        System.out.println("  修改后 pos: " + extractJsonValue(valueJson, "pos"));
                    }
                    if (valueJson.contains("\"event\":")) {
                        System.out.println("  修改后 event: " + extractJsonValue(valueJson, "event"));
                    }
                    
                    // 验证修改后的JSON格式
                    if (!isValidJson(valueJson)) {
                        System.out.println("  ⚠ 警告: 修改后的JSON格式可能不正确!");
                    } else {
                        System.out.println("  ✓ JSON格式验证通过");
                    }
                }
            }

            // 写回文件
            writeOffsetFile(filePath, offsetMap);
            System.out.println("\n✓ Offset操作成功!");
            if (backupFile != null) {
                System.out.println("备份文件: " + backupFile.getAbsolutePath());
            }

        } catch (IllegalArgumentException e) {
            System.out.println("\n✗ 参数错误: " + e.getMessage());
            System.out.println("请检查参数是否正确，使用 --help 查看使用说明");
        } catch (Exception e) {
            System.out.println("\n✗ Offset操作失败: " + e.getMessage());
            e.printStackTrace();
            if (backupFile != null && backupFile.exists()) {
                System.out.println("\n尝试恢复备份文件...");
                try {
                    restoreBackup(filePath, backupFile);
                    System.out.println("已恢复到备份文件");
                } catch (Exception restoreEx) {
                    System.out.println("恢复失败，请手动处理: " + restoreEx.getMessage());
                }
            }
        }
    }

    /**
     * 创建新的offset value JSON字符串
     * 根据参数构建符合Debezium格式的offset JSON
     */
    private static String createOffsetValueJson(Map<String, String> params) {
        StringBuilder json = new StringBuilder("{");
        
        // transaction_id (通常为null)
        json.append("\"transaction_id\":null");
        
        // ts_sec (时间戳，秒)
        String tsSec = params.getOrDefault("ts_sec", String.valueOf(System.currentTimeMillis() / 1000));
        json.append(",\"ts_sec\":").append(tsSec);
        
        // file (binlog文件名，必需)
        String file = params.get("file");
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("参数 'file' 是必需的，例如: file=mysql-bin.000001");
        }
        json.append(",\"file\":\"").append(file).append("\"");
        
        // pos (位置，必需)
        String pos = params.get("pos");
        if (pos == null || pos.isEmpty()) {
            throw new IllegalArgumentException("参数 'pos' 是必需的，例如: pos=123");
        }
        json.append(",\"pos\":").append(pos);
        
        // row (行号，默认为0)
        String row = params.getOrDefault("row", "0");
        json.append(",\"row\":").append(row);
        
        // server_id (服务器ID，默认为1)
        String serverId = params.getOrDefault("server_id", "1");
        json.append(",\"server_id\":").append(serverId);
        
        // event (事件编号，默认为0)
        String event = params.getOrDefault("event", "0");
        json.append(",\"event\":").append(event);
        
        // gtids (GTID集合，可选)
        String gtids = params.get("gtids");
        if (gtids != null && !gtids.isEmpty()) {
            json.append(",\"gtids\":\"").append(gtids).append("\"");
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * 备份offset文件
     */
    private static void backupOffsetFile(String sourcePath, File backupFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(sourcePath); FileOutputStream fos = new FileOutputStream(backupFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
    }

    /**
     * 从备份文件恢复
     */
    private static void restoreBackup(String targetPath, File backupFile) throws IOException {
        if (backupFile.exists()) {
            backupOffsetFile(backupFile.getAbsolutePath(), new File(targetPath));
        }
    }

    /**
     * 读取offset文件
     */
    private static Map<byte[], byte[]> readOffsetFile(String filePath) throws IOException, ClassNotFoundException {
        Map<byte[], byte[]> offsetMap = new HashMap<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            Object data = ois.readObject();
            if (data instanceof Map) {
                Map<?, ?> rawMap = (Map<?, ?>) data;
                for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                    if (entry.getKey() instanceof byte[] && entry.getValue() instanceof byte[]) {
                        offsetMap.put((byte[]) entry.getKey(), (byte[]) entry.getValue());
                    }
                }
            }
        }
        return offsetMap;
    }

    /**
     * 写入offset文件
     */
    private static void writeOffsetFile(String filePath, Map<byte[], byte[]> offsetMap) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(offsetMap);
        }
    }

    /**
     * 修改offset JSON字符串
     * 使用更可靠的JSON解析方式，避免多次修改导致的索引错误
     */
    private static String modifyOffsetJson(String json, Map<String, String> params) {
        // 从后往前修改，避免索引位置变化影响
        List<Map.Entry<String, String>> sortedParams = new ArrayList<>(params.entrySet());
        // 按字段在JSON中出现的顺序从后往前排序（简单策略：按key长度和位置）
        sortedParams.sort((a, b) -> {
            int posA = json.lastIndexOf("\"" + a.getKey() + "\":");
            int posB = json.lastIndexOf("\"" + b.getKey() + "\":");
            return Integer.compare(posB, posA); // 从后往前
        });

        String modified = json;
        
        // 特殊处理：如果修改了 pos 或 file，且存在 event 字段，自动重置 event 为 0
        // 除非用户明确指定了 event 参数
        boolean hasPosOrFile = params.containsKey("pos") || params.containsKey("file");
        boolean hasEventParam = params.containsKey("event");
        
        if (hasPosOrFile && !hasEventParam && modified.contains("\"event\":")) {
            System.out.println("  提示: 检测到修改了 pos 或 file，自动将 event 重置为 0");
            modified = replaceJsonField(modified, "event", "0");
        }
        
        for (Map.Entry<String, String> param : sortedParams) {
            String key = param.getKey();
            String value = param.getValue();

            // 根据参数类型修改JSON
            if ("file".equals(key) || "pos".equals(key) || "gtids".equals(key) || "ts_sec".equals(key) || "server_id".equals(key) || "event".equals(key)) {
                modified = replaceJsonField(modified, key, value);
            }
        }
        return modified;
    }
    
    /**
     * 简单验证JSON格式（检查大括号是否匹配）
     */
    private static boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }
        int braceCount = 0;
        boolean inString = false;
        for (int i = 0; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (ch == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
            } else if (!inString) {
                if (ch == '{') braceCount++;
                else if (ch == '}') braceCount--;
            }
        }
        return braceCount == 0 && !inString;
    }

    /**
     * 替换JSON中的字段值
     */
    private static String replaceJsonField(String json, String fieldName, String newValue) {
        String pattern = "\"" + fieldName + "\":";
        int patternStart = json.indexOf(pattern);
        
        if (patternStart == -1) {
            System.out.println("  警告: 未找到字段 '" + fieldName + "'，跳过修改");
            return json;
        }

        int valueStart = patternStart + pattern.length();
        
        // 跳过空格
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        if (valueStart >= json.length()) {
            System.out.println("  警告: 字段 '" + fieldName + "' 值位置超出范围，跳过修改");
            return json;
        }

        // 判断值的类型
        boolean isString = json.charAt(valueStart) == '"';
        int valueEnd;
        
        if (isString) {
            // 字符串值：找到结束引号
            valueStart++; // 跳过开始引号
            valueEnd = json.indexOf("\"", valueStart);
            if (valueEnd == -1) {
                System.out.println("  警告: 字段 '" + fieldName + "' 字符串值未正确结束，跳过修改");
                return json;
            }
            // 替换：保留字段名和冒号，替换值（带引号）
            String before = json.substring(0, valueStart);
            String after = json.substring(valueEnd);
            System.out.println("  修改字段 '" + fieldName + "' 从 '" + json.substring(valueStart, valueEnd) + "' 为: '" + newValue + "'");
            return before + newValue + after;
        } else {
            // 数字或其他值：找到逗号、右大括号或右中括号
            valueEnd = valueStart;
            while (valueEnd < json.length()) {
                char ch = json.charAt(valueEnd);
                if (ch == ',' || ch == '}' || ch == ']') {
                    break;
                }
                // 允许数字、负号、小数点、冒号（用于GTID格式）
                if (!Character.isDigit(ch) && ch != '-' && ch != '.' && ch != ':' && !Character.isWhitespace(ch)) {
                    break;
                }
                valueEnd++;
            }
            
            if (valueEnd > valueStart) {
                String oldValue = json.substring(valueStart, valueEnd).trim();
                String before = json.substring(0, valueStart);
                String after = json.substring(valueEnd);
                System.out.println("  修改字段 '" + fieldName + "' 从 '" + oldValue + "' 为: '" + newValue + "'");
                return before + newValue + after;
            } else {
                System.out.println("  警告: 字段 '" + fieldName + "' 值位置无效，跳过修改");
                return json;
            }
        }
    }

    /**
     * 尝试解析Kafka Connect特定的JSON格式offset
     * Kafka Connect将offset存储为Map<byte[], byte[]>格式，其中value是包含offset信息的JSON字符串
     */
    private static void parseKafkaConnectFormat(String filePath) {
        System.out.println("尝试解析为Kafka Connect offset格式...");

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            Object data = ois.readObject();

            if (data instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) data;
                System.out.println("\n=== Kafka Connect偏移量分析 ===");
                System.out.println("总记录数: " + map.size());

                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    System.out.println("\n========== 连接器偏移量记录 ==========");

                    // 解析key (通常是JSON字符串)
                    Object key = entry.getKey();
                    System.out.println("\n--- Key (连接器标识) ---");
                    if (key instanceof byte[]) {
                        parseKafkaConnectKey((byte[]) key);
                    } else {
                        System.out.println("Key: " + key);
                    }

                    // 解析value (JSON格式的offset信息)
                    Object value = entry.getValue();
                    System.out.println("\n--- Value (偏移量信息) ---");
                    if (value instanceof byte[]) {
                        parseKafkaConnectValue((byte[]) value);
                    } else {
                        System.out.println("Value: " + value);
                    }
                }
            } else {
                System.out.println("未知的数据类型: " + data.getClass().getName());
            }

        } catch (Exception e) {
            System.out.println("Kafka Connect格式解析错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 解析Kafka Connect的Key (通常是连接器名称)
     */
    private static void parseKafkaConnectKey(byte[] keyBytes) {
        try {
            String keyJson = new String(keyBytes, "UTF-8");
            System.out.println("Key JSON字符串: " + keyJson);

            // Kafka Connect key格式: {"schema":null,"payload":["连接器名称", {"server":"服务器名"}]}
            System.out.println("原始字节 (十六进制): ");
            for (int i = 0; i < Math.min(48, keyBytes.length); i++) {
                System.out.printf("%02x ", keyBytes[i] & 0xFF);
            }
            System.out.println();
        } catch (Exception e) {
            System.out.println("解析Key失败: " + e.getMessage());
        }
    }

    /**
     * 解析Kafka Connect的Value (包含binlog位置等offset信息)
     */
    private static void parseKafkaConnectValue(byte[] valueBytes) {
        try {
            String valueJson = new String(valueBytes, "UTF-8");
            System.out.println("Value JSON字符串: " + valueJson);

            // 提取关键信息
            System.out.println("\n关键偏移量信息:");
            if (valueJson.contains("\"file\":")) {
                String file = extractJsonValue(valueJson, "file");
                System.out.println("  Binlog文件: " + file);
            }
            if (valueJson.contains("\"pos\":")) {
                String pos = extractJsonValue(valueJson, "pos");
                System.out.println("  位置(Position): " + pos);
            }
            if (valueJson.contains("\"gtids\":")) {
                String gtids = extractJsonValue(valueJson, "gtids");
                System.out.println("  GTID集合: " + gtids);
            }
            if (valueJson.contains("\"server_id\":")) {
                String serverId = extractJsonValue(valueJson, "server_id");
                System.out.println("  服务器ID: " + serverId);
            }
            if (valueJson.contains("\"ts_sec\":")) {
                String tsSec = extractJsonValue(valueJson, "ts_sec");
                System.out.println("  时间戳: " + tsSec + " (" + new Date(Long.parseLong(tsSec) * 1000) + ")");
            }

//            System.out.println("\n原始字节 (前64字节, 十六进制):");
//            for (int i = 0; i < Math.min(64, valueBytes.length); i++) {
//                if (i % 16 == 0) System.out.print("  ");
//                System.out.printf("%02x ", valueBytes[i] & 0xFF);
//                if ((i + 1) % 16 == 0) System.out.println();
//            }
//            if (valueBytes.length <= 64) System.out.println();

        } catch (Exception e) {
            System.out.println("解析Value失败: " + e.getMessage());
        }
    }

    /**
     * 查找binlog事件的起始位置
     * 当Debezium记录的pos是事件结束位置时，需要找到事件的起始位置
     */
    private static void findBinlogStartPosition(String binlogFile, long position, Map<String, String> connParams) {
        System.out.println("\n========== 查找Binlog事件起始位置 ==========");
        System.out.println("Binlog文件: " + binlogFile);
        System.out.println("给定位置: " + position);
        System.out.println("说明: 给定位置通常是事件的结束位置，需要找到事件的起始位置");
        
        // 尝试通过JDBC连接MySQL查找
        String host = connParams.getOrDefault("host", "192.168.239.128");
        String port = connParams.getOrDefault("port", "3306");
        String user = connParams.getOrDefault("user", "root");
        String password = connParams.getOrDefault("password", "root");
        
        System.out.println("\n尝试连接MySQL: " + host + ":" + port);
        
        try {
            // 尝试使用反射加载MySQL驱动（如果类路径中有的话）
            Class<?> driverClass = Class.forName("com.mysql.cj.jdbc.Driver");
            java.sql.Driver driver = (java.sql.Driver) driverClass.getDeclaredConstructor().newInstance();
            
            String url = "jdbc:mysql://" + host + ":" + port + "/?useSSL=false&serverTimezone=UTC";
            try (java.sql.Connection conn = java.sql.DriverManager.getConnection(url, user, password);
                 java.sql.Statement stmt = conn.createStatement()) {
                
                System.out.println("✓ MySQL连接成功");
                
                // 查找包含指定位置的事件
                // SHOW BINLOG EVENTS 的 FROM 参数必须是事件边界位置，不能是任意位置
                // 策略：从文件开头（4，binlog文件前4字节是magic number）开始查找
                long searchStart = 4; // binlog文件从第4字节开始是有效数据
                
                // 如果目标位置较大，使用更大的LIMIT值以确保能找到
                int limit = position > 10000 ? 500 : 200;
                String sql = "SHOW BINLOG EVENTS IN '" + binlogFile + "' FROM " + searchStart + " LIMIT " + limit;
                
                System.out.println("\n执行SQL: " + sql);
                System.out.println("说明: FROM 参数必须是事件边界位置，所以从文件开头(4)开始查找");
                System.out.println("提示: 如果事件很多，可能需要增加LIMIT值或使用mysqlbinlog工具");
                
                try (java.sql.ResultSet rs = stmt.executeQuery(sql)) {
                    long eventStartPos = -1;
                    long eventEndPos = -1;
                    String eventType = null;
                    boolean found = false;
                    
                    System.out.println("\n查找结果:");
                    System.out.println("位置范围 | 事件类型 | 说明");
                    System.out.println("---------|---------|--------");
                    
                    int displayedCount = 0;
                    while (rs.next()) {
                        long pos = rs.getLong("Pos");
                        long endLogPos = rs.getLong("End_log_pos");
                        String eventTypeValue = rs.getString("Event_type");
                        
                        // 检查给定位置是否在这个事件范围内
                        if (pos <= position && position <= endLogPos) {
                            eventStartPos = pos;
                            eventEndPos = endLogPos;
                            eventType = eventTypeValue;
                            found = true;
                            System.out.printf("  [找到] %d-%d | %s | 包含位置 %d\n", pos, endLogPos, eventType, position);
                            break;
                        } else if (endLogPos > position) {
                            // 如果已经超过给定位置，说明没找到包含该位置的事件
                            System.out.printf("  %d-%d | %s | 已超过位置 %d，未找到包含该位置的事件\n", pos, endLogPos, eventTypeValue, position);
                            break;
                        }
                        
                        // 显示前几个事件和接近目标位置的事件用于参考
                        if (displayedCount < 5 || (pos <= position && position - endLogPos < 500)) {
                            System.out.printf("  %d-%d | %s |\n", pos, endLogPos, eventTypeValue);
                            displayedCount++;
                        }
                    }
                    
                    if (found) {
                        System.out.println("\n✓ 找到包含位置 " + position + " 的事件:");
                        System.out.println("  事件起始位置: " + eventStartPos);
                        System.out.println("  事件结束位置: " + eventEndPos);
                        System.out.println("  事件类型: " + eventType);
                        System.out.println("\n建议: 使用起始位置 " + eventStartPos + " 作为 offset 的 pos 值");
                        System.out.println("重置命令示例:");
                        System.out.println("  --reset <offset文件> file=" + binlogFile + " pos=" + eventStartPos);
                    } else {
                        System.out.println("\n⚠ 未找到包含位置 " + position + " 的事件");
                        System.out.println("可能原因:");
                        System.out.println("  1. 位置值不正确");
                        System.out.println("  2. binlog文件已轮转或删除");
                        System.out.println("  3. 位置不在当前binlog文件中");
                        printManualSqlCommands(binlogFile, position);
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            System.out.println("⚠ MySQL JDBC驱动未找到，无法自动连接MySQL");
            System.out.println("请手动执行以下SQL命令查找事件起始位置:\n");
            printManualSqlCommands(binlogFile, position);
        } catch (Exception e) {
            System.out.println("⚠ 连接MySQL失败: " + e.getMessage());
            System.out.println("请手动执行以下SQL命令查找事件起始位置:\n");
            printManualSqlCommands(binlogFile, position);
        }
    }
    
    /**
     * 打印手动执行的SQL命令
     */
    private static void printManualSqlCommands(String binlogFile, long position) {
        System.out.println("方法1: 使用 SHOW BINLOG EVENTS 命令（推荐）");
        System.out.println("----------------------------------------");
        System.out.println("连接到MySQL后执行:");
        System.out.println("  SHOW BINLOG EVENTS IN '" + binlogFile + "' FROM 4 LIMIT 200;");
        System.out.println("\n重要提示:");
        System.out.println("  - FROM 参数必须是事件边界位置，不能是任意位置");
        System.out.println("  - binlog文件从第4字节开始是有效数据，所以从4开始");
        System.out.println("  - 如果 FROM 参数不是事件边界，会报错: Wrong offset or I/O error");
        System.out.println("\n查找包含位置 " + position + " 的事件:");
        System.out.println("  - 在结果中找到 End_log_pos >= " + position + " 且 Pos <= " + position + " 的事件");
        System.out.println("  - 该事件的 Pos 值就是起始位置");
        System.out.println("\n方法2: 使用 mysqlbinlog 工具");
        System.out.println("----------------------------------------");
        System.out.println("在命令行执行:");
        System.out.println("  mysqlbinlog --start-position=4 --stop-position=" + (position + 100) + " " + binlogFile);
        System.out.println("\n查找包含位置 " + position + " 的事件，事件的起始位置就是正确的 pos 值");
        System.out.println("\n方法3: 如果知道大概位置，可以尝试从更早的事件边界开始");
        System.out.println("----------------------------------------");
        System.out.println("例如，如果知道位置大约在1000-2000之间，可以:");
        System.out.println("  1. 先执行: SHOW BINLOG EVENTS IN '" + binlogFile + "' FROM 4 LIMIT 50;");
        System.out.println("  2. 找到接近目标位置的事件边界（比如 Pos=1000）");
        System.out.println("  3. 再执行: SHOW BINLOG EVENTS IN '" + binlogFile + "' FROM 1000 LIMIT 50;");
    }
    
    /**
     * 从JSON字符串中提取字段值 (简单提取)
     */
    private static String extractJsonValue(String json, String fieldName) {
        try {
            String pattern = "\"" + fieldName + "\":";
            int start = json.indexOf(pattern);
            if (start == -1) return "N/A";

            start += pattern.length();
            // 跳过空格
            while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;

            // 查找值结束位置
            int end = start;
            if (json.charAt(start) == '"') {
                // 字符串值
                start++; // 跳过引号
                end = json.indexOf("\"", start);
            } else {
                // 数字或其他值
                while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-' || json.charAt(end) == ':')) {
                    end++;
                }
                // 找到逗号或括号
                int comma = json.indexOf(",", start);
                int brace = json.indexOf("}", start);
                end = Math.min(comma != -1 ? comma : Integer.MAX_VALUE, brace != -1 ? brace : Integer.MAX_VALUE);
            }

            if (end == -1 || end >= json.length()) return "N/A";
            return json.substring(start, end);
        } catch (Exception e) {
            return "N/A";
        }
    }
}