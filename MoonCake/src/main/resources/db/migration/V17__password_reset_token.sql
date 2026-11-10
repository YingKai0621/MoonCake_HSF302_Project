-- Customer "forgot password" flow. Only the SHA-256 hex of the emailed token is stored.
CREATE TABLE dbo.PASSWORD_RESET_TOKEN (
    token_id BIGINT IDENTITY(1,1) NOT NULL,
    account_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    created_at DATETIME2(3) NOT NULL DEFAULT (SYSUTCDATETIME()),
    expires_at DATETIME2(3) NOT NULL,
    used_at DATETIME2(3) NULL,
    CONSTRAINT PK_PASSWORD_RESET_TOKEN PRIMARY KEY CLUSTERED (token_id),
    CONSTRAINT FK_PASSWORD_RESET_TOKEN_account FOREIGN KEY (account_id) REFERENCES dbo.ACCOUNT(account_id),
    CONSTRAINT UQ_PASSWORD_RESET_TOKEN_hash UNIQUE (token_hash),
    CONSTRAINT CK_PASSWORD_RESET_TOKEN_expiry CHECK (expires_at > created_at),
    CONSTRAINT CK_PASSWORD_RESET_TOKEN_used CHECK (used_at IS NULL OR used_at >= created_at)
);

CREATE INDEX IX_PASSWORD_RESET_TOKEN_account ON dbo.PASSWORD_RESET_TOKEN(account_id, created_at);
