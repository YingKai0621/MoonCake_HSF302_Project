-- Run this script once with a login that can create databases.
-- It is intentionally separate from Flyway migrations.
-- Safe to rerun: an existing MoonCakeDB is left unchanged.

USE master;
GO

IF DB_ID(N'MoonCakeDB') IS NULL
BEGIN
    CREATE DATABASE MoonCakeDB;
    PRINT N'Created database MoonCakeDB.';
END
ELSE
BEGIN
    PRINT N'Database MoonCakeDB already exists; no changes were made.';
END;
GO

