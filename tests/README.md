# 回归测试

本目录完全由 AI 生成，仅使用合成账号、交易与课程数据，不连接真实学校账号。

账单脚本：在仓库根目录执行 `node tests/test_account_sync.cjs`。

Java 测试：先运行 `python scripts/build.py`。将单独获取的 `org.json:json:20240303` JAR 放在仓库之外，然后执行 `python scripts/test.py --json-jar /path/to/json-20240303.jar`。测试代码在桌面 JVM 执行，Android SDK 的 android.jar 仅用于解析平台类型；不会在本机请求学校服务或显示通知。

测试覆盖课程合并与冲突、账单分页及退款、小时/日缓存边界、短滑单页导航、URL 白名单和提醒时间计算。不能替代真机、真实账号及系统通知交付测试。
