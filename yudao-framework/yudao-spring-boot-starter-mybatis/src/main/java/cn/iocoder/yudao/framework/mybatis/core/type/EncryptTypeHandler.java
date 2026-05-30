package cn.iocoder.yudao.framework.mybatis.core.type;

import cn.hutool.core.lang.Assert;
import cn.hutool.extra.spring.SpringUtil;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 字段加密 TypeHandler，基于 JDK AES 实现。
 * 可通过 mybatis-plus.encryptor.password 配置项设置密钥。
 *
 * @author 芋道源码
 */
public class EncryptTypeHandler extends BaseTypeHandler<String> {

    private static final String ENCRYPTOR_PROPERTY_NAME = "mybatis-plus.encryptor.password";
    private static final String AES_TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final String AES_PROVIDER = "SunJCE";

    private static byte[] aesKey;

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, encrypt(parameter));
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return decrypt(value);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return decrypt(value);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return decrypt(value);
    }

    public static String decrypt(String value) {
        if (value == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION, AES_PROVIDER);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(getEncryptorKey(), "AES"));
            return new String(cipher.doFinal(Base64.getDecoder().decode(value)), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt configured value", ex);
        }
    }

    public static String encrypt(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION, AES_PROVIDER);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(getEncryptorKey(), "AES"));
            return Base64.getEncoder().encodeToString(cipher.doFinal(rawValue.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt configured value", ex);
        }
    }

    private static byte[] getEncryptorKey() {
        if (aesKey != null) {
            return aesKey;
        }
        String password = SpringUtil.getProperty(ENCRYPTOR_PROPERTY_NAME);
        Assert.notEmpty(password, "配置项({}) 不能为空", ENCRYPTOR_PROPERTY_NAME);
        aesKey = password.getBytes(StandardCharsets.UTF_8);
        return aesKey;
    }

}
