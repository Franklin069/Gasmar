package com.gasflow.gasflow.enums;

public enum StatusProcesso {
    AGUARDANDO_ANALISE,
    AGUARDANDO_RECEBIMENTO_EXECUCAO,
    AGUARDANDO_SOLICITACAO_PAGAMENTO,
    AGUARDANDO_AUTORIZACAO_PAGAMENTO,
    AGUARDANDO_VALIDACAO_MATERIAL,
    ENCERRADO,
    RECEBIDO_NAO_CONFORME,
    SOLICITACAO_NAO_CONFORME,
    NF_NAO_CONFORME;

    public String getDescricao() {
        return switch (this) {
            case AGUARDANDO_ANALISE -> "Aguardando análise";
            case AGUARDANDO_RECEBIMENTO_EXECUCAO -> "Aguardando recebimento/execução";
            case AGUARDANDO_SOLICITACAO_PAGAMENTO -> "Aguardando solicitação de pagamento";
            case AGUARDANDO_AUTORIZACAO_PAGAMENTO -> "Aguardando autorização de pagamento";
            case AGUARDANDO_VALIDACAO_MATERIAL -> "Aguardando validação do material";
            case ENCERRADO -> "Encerrado";
            case RECEBIDO_NAO_CONFORME -> "Recebido não conforme";
            case SOLICITACAO_NAO_CONFORME -> "Solicitação não conforme";
            case NF_NAO_CONFORME -> "Nota fiscal não conforme";
        };
    }

    public String getDescricaoTimeline() {
        return switch (this) {
            case AGUARDANDO_ANALISE -> "Protocolo Iniciado";
            case AGUARDANDO_RECEBIMENTO_EXECUCAO -> "Contratação Realizada";
            case AGUARDANDO_SOLICITACAO_PAGAMENTO -> "Pronto para Pagamento";
            case AGUARDANDO_AUTORIZACAO_PAGAMENTO -> "Pagamento Solicitado";
            case AGUARDANDO_VALIDACAO_MATERIAL -> "Material Entregue";
            case ENCERRADO -> "Processo Finalizado";
            case RECEBIDO_NAO_CONFORME -> "Material Inconforme";
            case SOLICITACAO_NAO_CONFORME -> "PABS Inconforme";
            case NF_NAO_CONFORME -> "NF Inconforme";
        };
    }
}