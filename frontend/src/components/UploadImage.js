import React, { useState } from 'react';
import { Box, Heading, Input, Button, Flex, Text, useToast } from '@chakra-ui/react';
import { Link } from 'react-router-dom';
import { FaArrowLeft, FaUpload } from 'react-icons/fa';

const API_BASE = 'http://localhost:8081';

const UploadImage = () => {
  const toast = useToast();
  const [title, setTitle] = useState('');
  const [file, setFile] = useState(null);
  const [isUploading, setIsUploading] = useState(false);

  const onFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      setFile(e.target.files[0]);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!file) {
      toast({
        title: 'No file selected',
        description: 'Please choose an image to upload.',
        status: 'warning',
        duration: 3000,
        isClosable: true,
      });
      return;
    }

    if (!title.trim()) {
      toast({
        title: 'Title required',
        description: 'Please enter a title for your image.',
        status: 'warning',
        duration: 3000,
        isClosable: true,
      });
      return;
    }

    try {
      setIsUploading(true);
      const formData = new FormData();
      formData.append('title', title);
      formData.append('file', file); // backend expects "file" param

      const res = await fetch(`${API_BASE}/images/upload`, {
        method: 'POST',
        body: formData,
      });

      if (!res.ok) {
        const txt = await res.text();
        throw new Error(`Upload failed (${res.status}) ${txt}`);
      }

      toast({
        title: 'Upload successful',
        description: 'Your image has been uploaded.',
        status: 'success',
        duration: 3000,
        isClosable: true,
      });

      // reset form
      setTitle('');
      setFile(null);
      // optionally, navigate back to gallery or just keep on page
      // window.location.href = '/';
    } catch (err) {
      console.error('Upload error:', err);
      toast({
        title: 'Upload failed',
        description: 'The server rejected the upload. Check the backend logs.',
        status: 'error',
        duration: 4000,
        isClosable: true,
      });
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <Flex direction="column" alignItems="center" bg="gray.50" minH="100vh" width="1000px" mx="auto">
      <Box maxW="800px" width="100%" p="6" mt="6" boxShadow="md" borderRadius="md" bg="white">
        <Flex justifyContent="space-between" alignItems="center" mb="4">
          <Heading as="h1" size="lg">Upload Image</Heading>
          <Link to="/">
            <Button leftIcon={<FaArrowLeft />}>Back to Gallery</Button>
          </Link>
        </Flex>

        <form onSubmit={handleSubmit}>
          <Box mb="4">
            <Text mb="2" fontWeight="semibold">Title</Text>
            <Input
              placeholder="Enter a title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />
          </Box>

          <Box mb="6">
            <Text mb="2" fontWeight="semibold">Image file</Text>
            {/* Native input for file selection works best */}
            <input type="file" accept="image/*" onChange={onFileChange} />
            {file && (
              <Text mt="2" fontSize="sm" color="gray.600">
                Selected: {file.name}
              </Text>
            )}
          </Box>

          <Button
            type="submit"
            colorScheme="green"
            leftIcon={<FaUpload />}
            isLoading={isUploading}
            loadingText="Uploading..."
          >
            Upload
          </Button>
        </form>
      </Box>
    </Flex>
  );
};

export default UploadImage;
