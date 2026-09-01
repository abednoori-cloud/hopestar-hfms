-- =====================================================================
-- V15__branch_website_logo.sql
-- Organization Settings module.
--
-- Adds website/logo_path to branches rather than introducing a separate
-- Settings/Organization entity -- per the design intent already
-- documented on BranchService's Javadoc ("there is no dedicated Settings
-- module yet ... the existing Branch/HQ record is the source of truth
-- for this until one is built"). The single headquarters branch row
-- becomes the editable "Organization Settings" record.
--
-- logo_path stores a FileStorageService-managed path (same convention as
-- student_documents.file_path, VARCHAR(500)) to an uploaded logo image,
-- replacing the previously hardcoded classpath logo when set. NULL means
-- "no logo uploaded yet -- LogoServiceImpl falls back to the classpath
-- default".
-- =====================================================================

SET NAMES utf8mb4;

ALTER TABLE branches
    ADD COLUMN website   VARCHAR(255) NULL AFTER email,
    ADD COLUMN logo_path VARCHAR(500) NULL AFTER website;
