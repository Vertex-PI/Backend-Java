package main.java.com.backend.banco;

import main.java.com.backend.Main;
import main.java.com.backend.leituraExcel.Energia;
import main.java.com.backend.leituraExcel.LeitorExcel;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class InserirNoBanco {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    Conexao conexao = new Conexao();
    JdbcTemplate con = conexao.getConexaoDoBanco();

    public void inserirDados() throws IOException{
        String nomeArquivo = "qlikview-consumo-de-energia-2024.xlsx";
        Path caminho = Path.of(nomeArquivo);
        InputStream arquivo = Files.newInputStream(caminho);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        LeitorExcel leitorExcel = new LeitorExcel();
        List<Energia> energiaExtraida = leitorExcel.extrairEnergia(nomeArquivo, arquivo);

        for (Energia energia : energiaExtraida) {
            String checkSql = "SELECT COUNT(*) FROM Energia WHERE Mes = ? AND Ano = ? AND local = ?";
            Integer count = con.queryForObject(checkSql, Integer.class, energia.getMes(), energia.getAno(), energia.getLocal());

            if (count != null && count == 0) {
                String sql = "INSERT INTO Energia (Kwh, Gasto, Mes, local, ano) VALUES (?, ?, ?, ?, ?)";
                try {
                    con.update(sql, energia.getKwh(), energia.getGasto(), energia.getMes(), energia.getLocal(), energia.getAno());

                    String dataHora = LocalDateTime.now().format(formatter);

                    String successMessage = String.format("Inserção bem-sucedida para: %s", energia);
                    String successMessageColored = String.format("\u001B[32m[%s] Inserção bem-sucedida para: %s\u001B[0m", dataHora, energia);

                    System.out.println(successMessageColored);

                    String logSql = "INSERT INTO Logs (descricao, data) VALUES (?, ?)";
                    con.update(logSql, successMessage, LocalDateTime.now());

                } catch (Exception e) {
                    String dataHora = LocalDateTime.now().format(formatter);
                    String errorMessage = String.format("Falha ao inserir energia: %s", energia);

                    logger.log(Level.SEVERE, errorMessage, e);

                    String logSql = "INSERT INTO Logs (descricao, data) VALUES (?, ?)";
                    con.update(logSql, errorMessage, LocalDateTime.now());
                }
            } else {
                String dataHora = LocalDateTime.now().format(formatter);
                String infoMessage = String.format("Registro já existente para: %s", energia);
                String infoMessageColored = String.format("\u001B[33m[%s] Registro já existente para: %s\u001B[0m", dataHora, energia);

                System.out.println(infoMessageColored);
            }
        }
    }
}
