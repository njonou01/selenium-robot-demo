package com.example.seleniumdemo.workflows;

import com.example.seleniumdemo.custom.reporting.WorkflowResult;

public record BankingResult(String accountNumber) implements WorkflowResult {
}
