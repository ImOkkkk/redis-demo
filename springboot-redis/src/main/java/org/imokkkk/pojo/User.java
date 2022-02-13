package org.imokkkk.pojo;

import java.io.Serializable;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ImOkkkk
 * @date 2022/2/13 20:10
 * @since 1.0
 */
@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User implements Serializable {
    private String name;

    private Integer age;
}
