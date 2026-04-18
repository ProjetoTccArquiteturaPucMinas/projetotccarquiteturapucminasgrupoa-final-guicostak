package com.example.marketplace.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.example.marketplace.model.*;
import org.springframework.stereotype.Service;

import com.example.marketplace.repository.ProdutoRepository;

@Service
public class ServicoCarrinho {

    private static final int PERCENTUAL_MAXIMO = 25;
    private static final BigDecimal CEM = new BigDecimal("100");

    private final ProdutoRepository repositorioProdutos;

    public ServicoCarrinho(ProdutoRepository repositorioProdutos) {
        this.repositorioProdutos = repositorioProdutos;
    }

    public ResumoCarrinho construirResumo(List<SelecaoCarrinho> selecoes) {

        List<ItemCarrinho> itens = new ArrayList<>();
        int quantidadeTotalItens = 0;

        for (SelecaoCarrinho selecao : selecoes) {
            Produto produto = repositorioProdutos.buscarPorId(selecao.getProdutoId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Produto não encontrado: " + selecao.getProdutoId()));

            itens.add(new ItemCarrinho(produto, selecao.getQuantidade()));
            quantidadeTotalItens += selecao.getQuantidade();
        }

        BigDecimal subtotal = itens.stream()
                .map(ItemCarrinho::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int percentualCategoria = 0;
        for (ItemCarrinho item : itens) {
            percentualCategoria += getPercentualDescontoPorCategoria(item.getProduto()) * item.getQuantidade();
        }

        int percentualQuantidade = getPercentualDescontoPorQuantidade(quantidadeTotalItens);
        int percentualTotal = percentualCategoria + percentualQuantidade;

        if (percentualTotal > PERCENTUAL_MAXIMO) {
            percentualTotal = PERCENTUAL_MAXIMO;
        }

        BigDecimal percentualDesconto = BigDecimal.valueOf(percentualTotal);
        BigDecimal valorDesconto = subtotal.multiply(percentualDesconto)
                .divide(CEM, 2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.subtract(valorDesconto);

        return new ResumoCarrinho(itens, subtotal, percentualDesconto, valorDesconto, total);
    }

    private static int getPercentualDescontoPorQuantidade(int quantidadeTotal) {
        if (quantidadeTotal <= 1) return 0;
        if (quantidadeTotal == 2) return 5;
        if (quantidadeTotal == 3) return 7;
        return 10;
    }

    private static int getPercentualDescontoPorCategoria(Produto produto) {
        switch (produto.getCategoria()) {
            case CAPINHA: return 3;
            case CARREGADOR: return 5;
            case FONE: return 3;
            case PELICULA: return 2;
            case SUPORTE: return 2;
            default: return 0;
        }
    }
}
