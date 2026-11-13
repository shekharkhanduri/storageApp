"use client";

import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useState } from "react";
import Image from "next/image";
import { actionsDropdownItems } from "@/constants";
import Link from "next/link";
import { constructDownloadUrl } from "@/lib/utils";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import {
  deleteFile,
  deleteMinioFile,
  getMinioDownloadUrl,
  renameFile,
  renameMinioFile,
  shareMinioFile,
  updateFileUsers,
} from "@/lib/actions/file.actions";
import { usePathname } from "next/navigation";
import { FileDetails, ShareInput } from "@/components/files/ActionsModalContent";

const getBaseName = (name: string, extension: string) => {
  if (extension && name.toLowerCase().endsWith(`.${extension.toLowerCase()}`)) {
    return name.slice(0, -(extension.length + 1));
  }

  return name;
};

const ActionDropdown = ({ file }: { file: UnifiedFile }) => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const [action, setAction] = useState<ActionType | null>(null);
  const [name, setName] = useState(getBaseName(file.name, file.extension));
  const [isLoading, setIsLoading] = useState(false);
  const [emails, setEmails] = useState<string[]>([]);

  const path = usePathname();
  const isMinio = file.storageBackend === "minio";

  const closeAllModals = () => {
    setIsModalOpen(false);
    setIsDropdownOpen(false);
    setAction(null);
    setName(getBaseName(file.name, file.extension));
    //   setEmails([]);
  };

  const handleAction = async () => {
    if (!action) return;
    setIsLoading(true);
    let success = false;

    if (action.value === "rename") {
      if (isMinio) {
        const result = await renameMinioFile({
          fileId: file.$id,
          name,
          extension: file.extension,
          path,
        });
        success = result.success;
      } else {
        success = Boolean(
          await renameFile({
            fileId: file.$id,
            name,
            extension: file.extension,
            path,
          }),
        );
      }
    }

    if (action.value === "share") {
      if (isMinio) {
        const result = await shareMinioFile({
          fileId: file.$id,
          emails,
          path,
        });
        success = result.success;
      } else {
        success = Boolean(
          await updateFileUsers({ fileId: file.$id, emails, path }),
        );
      }
    }

    if (action.value === "delete") {
      if (isMinio) {
        const result = await deleteMinioFile({ fileId: file.$id, path });
        success = result.success;
      } else if (file.bucketFileId) {
        success = Boolean(
          await deleteFile({
            fileId: file.$id,
            bucketFileId: file.bucketFileId,
            path,
          }),
        );
      }
    }

    if (success) closeAllModals();

    setIsLoading(false);
  };

  const handleRemoveUser = async (email: string) => {
    const updatedEmails = emails.filter((e) => e !== email);

    if (isMinio) {
      const result = await shareMinioFile({
        fileId: file.$id,
        emails: updatedEmails,
        path,
      });

      if (result.success) setEmails(updatedEmails);
    } else {
      const success = await updateFileUsers({
        fileId: file.$id,
        emails: updatedEmails,
        path,
      });

      if (success) setEmails(updatedEmails);
    }

    closeAllModals();
  };

  const handleDownload = async (
    event: React.MouseEvent<HTMLButtonElement>,
  ) => {
    event.preventDefault();
    event.stopPropagation();

    const url = await getMinioDownloadUrl(file.$id);
    if (url) window.location.assign(url);
  };

  const renderDialogContent = () => {
    if (!action) return null;

    const { value, label } = action;

    return (
      <DialogContent className="shad-dialog button">
        <DialogHeader className="flex flex-col gap-3">
          <DialogTitle className="text-center text-light-100">
            {label}
          </DialogTitle>
          {value === "rename" && (
            <Input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          )}
          {value === "details" && <FileDetails file={file} />}
          {value === "share" && (
            <ShareInput
              file={file}
              onInputChange={setEmails}
              onRemove={handleRemoveUser}
            />
          )}
          {value === "delete" && (
            <p className="delete-confirmation">
              Are you sure you want to delete{` `}
              <span className="delete-file-name">{file.name}</span>?
            </p>
          )}
        </DialogHeader>
        {["rename", "delete", "share"].includes(value) && (
          <DialogFooter className="flex flex-col gap-3 md:flex-row">
            <Button onClick={closeAllModals} className="modal-cancel-button">
              Cancel
            </Button>
            <Button onClick={handleAction} className="modal-submit-button">
              <p className="capitalize">{value}</p>
              {isLoading && (
                <Image
                  src="/assets/icons/loader.svg"
                  alt="loader"
                  width={24}
                  height={24}
                  className="animate-spin"
                />
              )}
            </Button>
          </DialogFooter>
        )}
      </DialogContent>
    );
  };

  return (
    <Dialog open={isModalOpen} onOpenChange={setIsModalOpen}>
      <DropdownMenu open={isDropdownOpen} onOpenChange={setIsDropdownOpen}>
        <DropdownMenuTrigger className="shad-no-focus">
          <Image
            src="/assets/icons/dots.svg"
            alt="dots"
            width={34}
            height={34}
          />
        </DropdownMenuTrigger>
        <DropdownMenuContent>
          <DropdownMenuLabel className="max-w-[200px] truncate">
            {file.name}
          </DropdownMenuLabel>
          <DropdownMenuSeparator />
          {actionsDropdownItems.map((actionItem) => (
            <DropdownMenuItem
              key={actionItem.value}
              className="shad-dropdown-item"
              onClick={() => {
                setAction(actionItem);

                if (
                  ["rename", "share", "delete", "details"].includes(
                    actionItem.value,
                  )
                ) {
                  setIsModalOpen(true);
                }
              }}
            >
              {actionItem.value === "download" ? (
                isMinio ? (
                  <button
                    type="button"
                    onClick={handleDownload}
                    className="flex items-center gap-2"
                  >
                    <Image
                      src={actionItem.icon}
                      alt={actionItem.label}
                      width={30}
                      height={30}
                    />
                    {actionItem.label}
                  </button>
                ) : (
                  <Link
                    href={constructDownloadUrl(file.bucketFileId || "")}
                    download={file.name}
                    className="flex items-center gap-2"
                  >
                    <Image
                      src={actionItem.icon}
                      alt={actionItem.label}
                      width={30}
                      height={30}
                    />
                    {actionItem.label}
                  </Link>
                )
              ) : (
                <div className="flex items-center gap-2">
                  <Image
                    src={actionItem.icon}
                    alt={actionItem.label}
                    width={30}
                    height={30}
                  />
                  {actionItem.label}
                </div>
              )}
            </DropdownMenuItem>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>

      {renderDialogContent()}
    </Dialog>
  );
};
export default ActionDropdown;
