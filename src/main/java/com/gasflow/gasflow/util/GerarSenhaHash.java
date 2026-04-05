package com.gasflow.gasflow.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GerarSenhaHash {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String senha = "123"; // mudar para a senha desejada

        String hash = encoder.encode(senha);

        System.out.println("Senha original: " + senha);
        System.out.println("Hash gerado: " + hash);
    }
}