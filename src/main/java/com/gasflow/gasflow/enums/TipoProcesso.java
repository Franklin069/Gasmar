package com.gasflow.gasflow.enums;

public enum TipoProcesso {
    COMPRA_BEM,
    CONTRATACAO_SERVICO;

    public String getDescricao() {
        return switch (this) {
            case COMPRA_BEM -> "Aquisição de Material";
            case CONTRATACAO_SERVICO -> "Contratação de Serviço";
        };
    }
}
