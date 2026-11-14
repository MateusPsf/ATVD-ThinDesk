package com.pfc.thindesk.dashboard;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public record DashboardFiltro(LocalDate inicio, LocalDate fim, String status, String tipo, String tecnico) {
    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    public static final String NAO_INFORMADO = "__nao_informado__";
    public static final List<String> STATUS = List.of("Aberto", "Em andamento", "Concluído", "Cancelado", NAO_INFORMADO);

    public static DashboardFiltro de(Map<String, String> parametros) {
        LocalDate inicio = data(parametros.get("dataInicio"));
        LocalDate fim = data(parametros.get("dataFim"));
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new IllegalArgumentException("A data inicial deve ser anterior ou igual à data final.");
        }
        String status = texto(parametros.get("status"));
        if (status != null && !STATUS.contains(status)) {
            throw new IllegalArgumentException("Status inválido.");
        }
        return new DashboardFiltro(inicio, fim, status, texto(parametros.get("tipo")), texto(parametros.get("tecnico")));
    }

    public String agrupamento(String valor) {
        if (valor == null || valor.isBlank()) {
            return inicio != null && fim != null && ChronoUnit.DAYS.between(inicio, fim) < 90 ? "dia" : "mes";
        }
        if (!List.of("dia", "mes").contains(valor)) {
            throw new IllegalArgumentException("Agrupamento deve ser dia ou mes.");
        }
        return valor;
    }

    public static int inteiro(Map<String, String> parametros, String nome, int padrao, int minimo, int maximo) {
        try {
            int valor = parametros.containsKey(nome) ? Integer.parseInt(parametros.get(nome)) : padrao;
            if (valor < minimo || valor > maximo) throw new NumberFormatException();
            return valor;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(nome + " deve ser um inteiro entre " + minimo + " e " + maximo + ".");
        }
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

    private static LocalDate data(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            if (!valor.matches("\\d{4}-\\d{2}-\\d{2}")) throw new DateTimeParseException("", valor, 0);
            LocalDate data = LocalDate.parse(valor);
            if (data.getYear() < 1 || data.getYear() > 9998) throw new DateTimeParseException("", valor, 0);
            return data;
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Data inválida. Use YYYY-MM-DD com ano entre 0001 e 9998.");
        }
    }
}
